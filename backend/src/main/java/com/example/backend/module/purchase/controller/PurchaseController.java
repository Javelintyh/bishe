package com.example.backend.module.purchase.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.api.ApiResponse;
import com.example.backend.module.purchase.dto.CreatePurchaseOrderRequest;
import com.example.backend.module.purchase.dto.PurchaseOrderVO;
import com.example.backend.module.purchase.entity.PurchaseOrder;
import com.example.backend.module.purchase.entity.PurchaseOrderDetail;
import com.example.backend.module.purchase.service.PurchaseOrderDetailService;
import com.example.backend.module.purchase.service.PurchaseOrderService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase/orders")
@PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
public class PurchaseController {
    private final PurchaseOrderService purchaseOrderService;
    private final PurchaseOrderDetailService purchaseOrderDetailService;

    public PurchaseController(PurchaseOrderService purchaseOrderService,
                              PurchaseOrderDetailService purchaseOrderDetailService) {
        this.purchaseOrderService = purchaseOrderService;
        this.purchaseOrderDetailService = purchaseOrderDetailService;
    }

    @GetMapping
    public ApiResponse<List<PurchaseOrderVO>> list(@RequestParam(required = false) String status) {
        LambdaQueryWrapper<PurchaseOrder> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            qw.eq(PurchaseOrder::getStatus, status);
        }
        qw.orderByDesc(PurchaseOrder::getId);
        List<PurchaseOrderVO> list = purchaseOrderService.list(qw).stream()
                .map(p -> new PurchaseOrderVO(p.getId(), p.getPoNo(), p.getSupplierId(), p.getStatus(),
                        p.getExpectedDate(), p.getRemark(), p.getCreatedAt(), p.getUpdatedAt()))
                .toList();
        return ApiResponse.ok(list);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PurchaseOrderVO> create(@Valid @RequestBody CreatePurchaseOrderRequest req) {
        Long id = purchaseOrderService.createOrder(req);
        PurchaseOrder p = purchaseOrderService.getById(id);
        PurchaseOrderVO vo = new PurchaseOrderVO(
                p.getId(), p.getPoNo(), p.getSupplierId(), p.getStatus(),
                p.getExpectedDate(), p.getRemark(), p.getCreatedAt(), p.getUpdatedAt()
        );
        return ApiResponse.ok(vo);
    }

    @PostMapping("/{id}/purchase")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> markPurchased(@PathVariable Long id) {
        purchaseOrderService.markPurchased(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/receive-all")
    @PreAuthorize("hasRole('WAREHOUSE')")
    public ApiResponse<Void> receiveAll(@PathVariable Long id) {
        purchaseOrderService.receiveAll(id);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/details")
    public ApiResponse<List<PurchaseOrderDetail>> details(@PathVariable Long id) {
        LambdaQueryWrapper<PurchaseOrderDetail> qw = new LambdaQueryWrapper<>();
        qw.eq(PurchaseOrderDetail::getPoId, id);
        List<PurchaseOrderDetail> list = purchaseOrderDetailService.list(qw);
        return ApiResponse.ok(list);
    }
}

