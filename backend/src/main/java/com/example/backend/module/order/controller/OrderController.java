package com.example.backend.module.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.api.ApiResponse;
import com.example.backend.common.exception.ApiException;
import com.example.backend.module.order.dto.CreateOrderRequest;
import com.example.backend.module.order.dto.OrderVO;
import com.example.backend.module.order.entity.OrderDetail;
import com.example.backend.module.order.entity.OrderMain;
import com.example.backend.module.order.service.OrderDetailService;
import com.example.backend.module.order.service.OrderMainService;
import com.example.backend.module.message.entity.MessageNotice;
import com.example.backend.module.message.service.MessageNoticeService;
import com.example.backend.module.production.entity.ProductionWorkOrder;
import com.example.backend.module.production.service.ProductionWorkOrderService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderMainService orderMainService;
    private final OrderDetailService orderDetailService;
    private final ProductionWorkOrderService workOrderService;
    private final MessageNoticeService messageNoticeService;

    public OrderController(
            OrderMainService orderMainService,
            OrderDetailService orderDetailService,
            ProductionWorkOrderService workOrderService,
            MessageNoticeService messageNoticeService
    ) {
        this.orderMainService = orderMainService;
        this.orderDetailService = orderDetailService;
        this.workOrderService = workOrderService;
        this.messageNoticeService = messageNoticeService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    public ApiResponse<List<OrderVO>> list(@RequestParam(required = false) String status) {
        LambdaQueryWrapper<OrderMain> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            qw.eq(OrderMain::getStatus, status);
        }
        qw.orderByDesc(OrderMain::getId);
        List<OrderMain> mains = orderMainService.list(qw);
        List<OrderVO> merged = merge(mains);
        // 置顶优先 => 加急优先 => 状态优先 => 交货时间越早越优先 => 工作量越大越优先
        merged.sort((a, b) -> {
            int pinnedCmp = Boolean.compare(b.pinned(), a.pinned());
            if (pinnedCmp != 0) return pinnedCmp;

            int urgentCmp = Boolean.compare(b.urgent(), a.urgent());
            if (urgentCmp != 0) return urgentCmp;

            int rankA = orderStatusRank(a.status());
            int rankB = orderStatusRank(b.status());
            int statusCmp = rankA - rankB;
            if (statusCmp != 0) return statusCmp;

            LocalDate ad = a.deliveryDate() == null ? LocalDate.MAX : a.deliveryDate();
            LocalDate bd = b.deliveryDate() == null ? LocalDate.MAX : b.deliveryDate();
            int dateCmp = ad.compareTo(bd); // 越早越前
            if (dateCmp != 0) return dateCmp;

            BigDecimal aq = a.qty() == null ? BigDecimal.ZERO : a.qty();
            BigDecimal bq = b.qty() == null ? BigDecimal.ZERO : b.qty();
            int qtyCmp = bq.compareTo(aq);
            if (qtyCmp != 0) return qtyCmp; // 工作量越大越前

            return 0;
        });

        return ApiResponse.ok(merged);
    }

    private int orderStatusRank(String status) {
        if (status == null) return 999;
        return switch (status) {
            case "PENDING" -> 0;
            case "PRODUCING" -> 1;
            case "COMPLETED" -> 2;
            case "SHIPPED" -> 3;
            case "DONE" -> 4;
            default -> 999;
        };
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    public ApiResponse<OrderVO> create(@Valid @RequestBody CreateOrderRequest req) {
        Long id = orderMainService.createOrder(req);
        return ApiResponse.ok(getOne(id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrderVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(getOne(id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    public ApiResponse<OrderVO> updateStatus(@PathVariable Long id, @RequestParam String status) {
        OrderMain main = orderMainService.getById(id);
        if (main == null) {
            throw new ApiException(404, "订单不存在");
        }
        // 补产订单（RP前缀）在成品入库后直接完结，不进入“待发货”
        if ("COMPLETED".equalsIgnoreCase(status) && main.getOrderNo() != null && main.getOrderNo().startsWith("RP")) {
            status = "DONE";
        }
        main.setStatus(status);
        // 订单已完成后，自动取消加急/置顶标记
        if ("COMPLETED".equalsIgnoreCase(status) || "DONE".equalsIgnoreCase(status)) {
            main.setUrgent(false);
            main.setPinned(false);
        }
        orderMainService.updateById(main);

        // 同步工单状态（订单驱动）：PENDING->TO_PRODUCE, 其余状态按同名同步
        ProductionWorkOrder wo = workOrderService.getOne(
                new LambdaQueryWrapper<ProductionWorkOrder>().eq(ProductionWorkOrder::getOrderId, id)
        );
        if (wo != null) {
            String woStatus = mapOrderStatusToWorkOrderStatus(status);
            if (woStatus != null && !woStatus.equalsIgnoreCase(wo.getStatus())) {
                wo.setStatus(woStatus);
                workOrderService.updateById(wo);
            }
        }

        // 订单完成后，清理对应工单未读加急消息，避免仓库端继续显示“加急待出库”
        if (("COMPLETED".equalsIgnoreCase(status) || "DONE".equalsIgnoreCase(status)) && wo != null && wo.getWorkOrderNo() != null) {
                List<MessageNotice> unreads = messageNoticeService.list(new LambdaQueryWrapper<MessageNotice>()
                        .eq(MessageNotice::getNoticeType, "WO_URGENT")
                        .eq(MessageNotice::getRelatedType, "WORK_ORDER")
                        .eq(MessageNotice::getRelatedId, wo.getWorkOrderNo())
                        .eq(MessageNotice::getIsRead, false));
                for (MessageNotice n : unreads) {
                    n.setIsRead(true);
                    messageNoticeService.updateById(n);
                }
        }
        return ApiResponse.ok(getOne(id));
    }

    private String mapOrderStatusToWorkOrderStatus(String orderStatus) {
        if (orderStatus == null) return null;
        return switch (orderStatus.toUpperCase()) {
            case "PENDING" -> "TO_PRODUCE";
            case "PRODUCING" -> "PRODUCING";
            case "COMPLETED" -> "COMPLETED";
            // 订单“已完成”不应把工单打回“待入库”链路，统一归并到完成态
            case "DONE" -> "COMPLETED";
            default -> null;
        };
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrderVO> update(@PathVariable Long id, @Valid @RequestBody CreateOrderRequest req) {
        OrderMain main = orderMainService.getById(id);
        if (main == null) {
            throw new ApiException(404, "订单不存在");
        }
        
        // 更新订单主表
        main.setCustomerId(req.customerId());
        main.setDeliveryDate(req.deliveryDate());

        // urgent 优先：urgent=true => pinned=true（保证“加急优先显示（置顶）”）
        if (req.urgent() != null) main.setUrgent(Boolean.TRUE.equals(req.urgent()));
        if (Boolean.TRUE.equals(main.isUrgent())) {
            main.setPinned(true);
        } else if (req.pinned() != null) {
            main.setPinned(Boolean.TRUE.equals(req.pinned()));
        }

        orderMainService.updateById(main);
        
        // 更新订单明细
        OrderDetail detail = orderDetailService.getOne(
            new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, id)
        );
        if (detail != null) {
            detail.setProductMaterialId(req.productMaterialId());
            detail.setQty(req.qty());
            orderDetailService.updateById(detail);
        }
        
        // 同步更新工单
        ProductionWorkOrder wo = workOrderService.getOne(
            new LambdaQueryWrapper<ProductionWorkOrder>().eq(ProductionWorkOrder::getOrderId, id)
        );
        if (wo != null) {
            wo.setProductMaterialId(req.productMaterialId());
            wo.setQty(req.qty());
            wo.setDueDate(req.deliveryDate());
            workOrderService.updateById(wo);
        }

        // urgent 改动同步到仓库端：创建/取消 WO_URGENT 消息
        if (wo != null) {
            boolean urgent = Boolean.TRUE.equals(main.isUrgent());
            if (urgent) {
                boolean woUrgentExists = messageNoticeService.exists(new LambdaQueryWrapper<MessageNotice>()
                        .eq(MessageNotice::getNoticeType, "WO_URGENT")
                        .eq(MessageNotice::getRelatedType, "WORK_ORDER")
                        .eq(MessageNotice::getRelatedId, wo.getWorkOrderNo())
                        .eq(MessageNotice::getIsRead, false));
                if (!woUrgentExists) {
                    MessageNotice n = new MessageNotice();
                    n.setNoticeType("WO_URGENT");
                    n.setTitle("加急订单待出库");
                    String deliveryText = main.getDeliveryDate() != null ? main.getDeliveryDate().toString() : "";
                    StringBuilder content = new StringBuilder();
                    content.append("工单号: ").append(wo.getWorkOrderNo()).append('\n');
                    if (!deliveryText.isBlank()) {
                        content.append("交货期: ").append(deliveryText);
                    }
                    n.setContent(content.toString());
                    n.setLevel("WARN");
                    n.setRelatedType("WORK_ORDER");
                    n.setRelatedId(wo.getWorkOrderNo());
                    n.setIsRead(false);
                    n.setCreatedAt(LocalDateTime.now());
                    messageNoticeService.save(n);
                }
            } else {
                // urgent=false：把该工单的未读 WO_URGENT 置为已读，避免仓库端误提示
                List<MessageNotice> unreads = messageNoticeService.list(new LambdaQueryWrapper<MessageNotice>()
                        .eq(MessageNotice::getNoticeType, "WO_URGENT")
                        .eq(MessageNotice::getRelatedType, "WORK_ORDER")
                        .eq(MessageNotice::getRelatedId, wo.getWorkOrderNo())
                        .eq(MessageNotice::getIsRead, false));
                for (MessageNotice n : unreads) {
                    n.setIsRead(true);
                    messageNoticeService.updateById(n);
                }
            }
        }
        
        return ApiResponse.ok(getOne(id));
    }

    @PutMapping("/{id}/pinned")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrderVO> setPinned(@PathVariable Long id, @RequestParam boolean pinned) {
        OrderMain main = orderMainService.getById(id);
        if (main == null) {
            throw new ApiException(404, "订单不存在");
        }
        main.setPinned(pinned);
        orderMainService.updateById(main);
        return ApiResponse.ok(getOne(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        OrderMain main = orderMainService.getById(id);
        if (main == null) {
            throw new ApiException(404, "订单不存在");
        }
        
        // 删除关联的工单
        workOrderService.remove(
            new LambdaQueryWrapper<ProductionWorkOrder>().eq(ProductionWorkOrder::getOrderId, id)
        );
        
        // 删除订单明细
        orderDetailService.remove(
            new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, id)
        );
        
        // 删除订单主表
        orderMainService.removeById(id);
        
        return ApiResponse.ok();
    }

    private OrderVO getOne(Long id) {
        OrderMain main = orderMainService.getById(id);
        if (main == null) {
            throw new ApiException(404, "订单不存在");
        }
        List<OrderVO> merged = merge(List.of(main));
        return merged.isEmpty() ? null : merged.get(0);
    }

    private List<OrderVO> merge(List<OrderMain> mains) {
        if (mains.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = mains.stream().map(OrderMain::getId).toList();

        Map<Long, OrderDetail> detailByOrderId = orderDetailService.list(
                        new LambdaQueryWrapper<OrderDetail>().in(OrderDetail::getOrderId, orderIds))
                .stream()
                .collect(Collectors.toMap(OrderDetail::getOrderId, Function.identity(), (a, b) -> a));

        Map<Long, ProductionWorkOrder> woByOrderId = workOrderService.list(
                        new LambdaQueryWrapper<ProductionWorkOrder>().in(ProductionWorkOrder::getOrderId, orderIds))
                .stream()
                .collect(Collectors.toMap(ProductionWorkOrder::getOrderId, Function.identity(), (a, b) -> a));

        List<OrderVO> result = new ArrayList<>(mains.size());
        for (OrderMain m : mains) {
            OrderDetail d = detailByOrderId.get(m.getId());
            ProductionWorkOrder wo = woByOrderId.get(m.getId());
            result.add(new OrderVO(
                    m.getId(),
                    m.getOrderNo(),
                    m.getCustomerId(),
                    m.getStatus(),
                    m.getDeliveryDate(),
                    m.isPinned(),
                    m.isUrgent(),
                    d == null ? null : d.getProductMaterialId(),
                    d == null ? null : d.getQty(),
                    wo == null ? null : wo.getWorkOrderNo(),
                    m.getCreatedAt(),
                    m.getUpdatedAt()
            ));
        }
        return result;
    }
}

