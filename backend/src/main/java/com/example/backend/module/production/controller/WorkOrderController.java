package com.example.backend.module.production.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.api.ApiResponse;
import com.example.backend.common.exception.ApiException;
import com.example.backend.module.base.entity.BaseBom;
import com.example.backend.module.base.service.BaseBomService;
import com.example.backend.module.inventory.entity.InventoryStock;
import com.example.backend.module.inventory.mapper.InventoryStockMapper;
import com.example.backend.module.order.entity.OrderMain;
import com.example.backend.module.order.service.OrderMainService;
import com.example.backend.module.production.entity.ProductionWorkOrder;
import com.example.backend.module.production.service.ProductionWorkOrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {
    private final ProductionWorkOrderService workOrderService;
    private final BaseBomService baseBomService;
    private final InventoryStockMapper inventoryStockMapper;
    private final OrderMainService orderMainService;

    public WorkOrderController(
            ProductionWorkOrderService workOrderService,
            BaseBomService baseBomService,
            InventoryStockMapper inventoryStockMapper,
            OrderMainService orderMainService
    ) {
        this.workOrderService = workOrderService;
        this.baseBomService = baseBomService;
        this.inventoryStockMapper = inventoryStockMapper;
        this.orderMainService = orderMainService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WORKSHOP','WAREHOUSE')")
    public ApiResponse<List<ProductionWorkOrder>> list(@RequestParam(required = false) String status) {
        LambdaQueryWrapper<ProductionWorkOrder> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            qw.eq(ProductionWorkOrder::getStatus, status);
        }
        qw.orderByDesc(ProductionWorkOrder::getId);
        List<ProductionWorkOrder> workOrders = workOrderService.list(qw);

        // 根据订单优先级进行返回排序：置顶 -> 加急 -> 状态 -> 交货时间 -> 工作量
        if (workOrders.isEmpty()) {
            return ApiResponse.ok(workOrders);
        }
        List<Long> orderIds = workOrders.stream().map(ProductionWorkOrder::getOrderId).distinct().toList();
        List<OrderMain> mains = orderMainService.list(
                new LambdaQueryWrapper<OrderMain>().in(OrderMain::getId, orderIds)
        );
        Map<Long, OrderMain> mainMap = mains.stream()
                .collect(Collectors.toMap(OrderMain::getId, m -> m, (a, b) -> a));

        workOrders.sort((a, b) -> {
            OrderMain am = mainMap.get(a.getOrderId());
            OrderMain bm = mainMap.get(b.getOrderId());

            boolean ap = am != null && am.isPinned();
            boolean bp = bm != null && bm.isPinned();
            int pinnedCmp = Boolean.compare(bp, ap);
            if (pinnedCmp != 0) return pinnedCmp;

            boolean au = am != null && am.isUrgent();
            boolean bu = bm != null && bm.isUrgent();
            int urgentCmp = Boolean.compare(bu, au);
            if (urgentCmp != 0) return urgentCmp;

            // 状态优先：待处理最优先，已完成最后
            String as = am == null ? null : am.getStatus();
            String bs = bm == null ? null : bm.getStatus();
            int statusCmp = orderStatusRank(as) - orderStatusRank(bs);
            if (statusCmp != 0) return statusCmp;

            LocalDate ad = a.getDueDate() == null ? LocalDate.MAX : a.getDueDate();
            LocalDate bd = b.getDueDate() == null ? LocalDate.MAX : b.getDueDate();
            int dateCmp = ad.compareTo(bd); // 越早越前
            if (dateCmp != 0) return dateCmp;

            BigDecimal aq = a.getQty() == null ? BigDecimal.ZERO : a.getQty();
            BigDecimal bq = b.getQty() == null ? BigDecimal.ZERO : b.getQty();
            int qtyCmp = bq.compareTo(aq);
            if (qtyCmp != 0) return qtyCmp; // 工作量越大越前
            return 0;
        });

        // 注入 urgent 给前端展示（不落库字段）
        for (ProductionWorkOrder wo : workOrders) {
            OrderMain m = mainMap.get(wo.getOrderId());
            wo.setUrgent(m != null && m.isUrgent());
        }

        return ApiResponse.ok(workOrders);
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

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WORKSHOP')")
    public ApiResponse<ProductionWorkOrder> detail(@PathVariable Long id) {
        ProductionWorkOrder wo = workOrderService.getById(id);
        if (wo == null) {
            throw new ApiException(404, "工单不存在");
        }
        return ApiResponse.ok(wo);
    }

    @GetMapping("/{id}/kit-check")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<KitCheckItem>> kitCheck(@PathVariable Long id) {
        ProductionWorkOrder wo = workOrderService.getById(id);
        if (wo == null) {
            throw new ApiException(404, "工单不存在");
        }
        return ApiResponse.ok(checkKit(wo));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','WORKSHOP','WAREHOUSE')")
    public ApiResponse<ProductionWorkOrder> updateStatus(@PathVariable Long id, @RequestParam String status) {
        ProductionWorkOrder wo = workOrderService.getById(id);
        if (wo == null) {
            throw new ApiException(404, "工单不存在");
        }
        wo.setStatus(status);
        workOrderService.updateById(wo);
        return ApiResponse.ok(wo);
    }

    @PutMapping("/{id}/produce")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    public ApiResponse<ProductionWorkOrder> updateForProduce(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam java.math.BigDecimal qty) {
        ProductionWorkOrder wo = workOrderService.getById(id);
        if (wo == null) {
            throw new ApiException(404, "工单不存在");
        }
        wo.setStatus(status);
        wo.setQty(qty);
        workOrderService.updateById(wo);
        return ApiResponse.ok(wo);
    }

    @PutMapping("/{id}/assignee")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductionWorkOrder> assign(@PathVariable Long id, @RequestParam Long assigneeUserId) {
        ProductionWorkOrder wo = workOrderService.getById(id);
        if (wo == null) {
            throw new ApiException(404, "工单不存在");
        }
        wo.setAssigneeUserId(assigneeUserId);
        workOrderService.updateById(wo);
        return ApiResponse.ok(wo);
    }

    private List<KitCheckItem> checkKit(ProductionWorkOrder wo) {
        List<BaseBom> boms = baseBomService.list(new LambdaQueryWrapper<BaseBom>()
                .eq(BaseBom::getProductMaterialId, wo.getProductMaterialId()));
        if (boms.isEmpty()) {
            return List.of();
        }

        List<Long> materialIds = boms.stream().map(BaseBom::getMaterialId).distinct().toList();
        Map<Long, InventoryStock> stockMap = inventoryStockMapper.selectList(new LambdaQueryWrapper<InventoryStock>()
                        .in(InventoryStock::getMaterialId, materialIds))
                .stream()
                .collect(Collectors.toMap(InventoryStock::getMaterialId, s -> s, (a, b) -> a));

        return boms.stream().map(b -> {
            BigDecimal required = b.getQty().multiply(wo.getQty());
            BigDecimal stock = stockMap.get(b.getMaterialId()) == null ? BigDecimal.ZERO : stockMap.get(b.getMaterialId()).getQty();
            BigDecimal shortage = required.subtract(stock);
            return new KitCheckItem(b.getMaterialId(), required, stock, shortage.compareTo(BigDecimal.ZERO) > 0 ? shortage : BigDecimal.ZERO);
        }).filter(i -> i.shortageQty().compareTo(BigDecimal.ZERO) > 0).toList();
    }

    public record KitCheckItem(
            Long materialId,
            BigDecimal requiredQty,
            BigDecimal stockQty,
            BigDecimal shortageQty
    ) {
    }
}

