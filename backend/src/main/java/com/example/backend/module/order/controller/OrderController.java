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
import com.example.backend.module.production.entity.ProductionWorkOrder;
import com.example.backend.module.production.service.ProductionWorkOrderService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderMainService orderMainService;
    private final OrderDetailService orderDetailService;
    private final ProductionWorkOrderService workOrderService;

    public OrderController(
            OrderMainService orderMainService,
            OrderDetailService orderDetailService,
            ProductionWorkOrderService workOrderService
    ) {
        this.orderMainService = orderMainService;
        this.orderDetailService = orderDetailService;
        this.workOrderService = workOrderService;
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
        return ApiResponse.ok(merge(mains));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
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
        main.setStatus(status);
        orderMainService.updateById(main);
        return ApiResponse.ok(getOne(id));
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

