package com.example.backend.module.purchase.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.api.ApiResponse;
import com.example.backend.module.purchase.dto.CreatePurchaseOrderRequest;
import com.example.backend.module.purchase.dto.PurchaseOrderVO;
import com.example.backend.module.purchase.entity.PurchaseOrder;
import com.example.backend.module.purchase.service.PurchaseOrderService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase/orders")
@PreAuthorize("hasRole('ADMIN')")
public class PurchaseController {
    private final PurchaseOrderService purchaseOrderService;

    public PurchaseController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
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
    public ApiResponse<PurchaseOrderVO> create(@Valid @RequestBody CreatePurchaseOrderRequest req) {
        Long id = purchaseOrderService.createOrder(req);
        PurchaseOrder p = purchaseOrderService.getById(id);
        PurchaseOrderVO vo = new PurchaseOrderVO(
                p.getId(), p.getPoNo(), p.getSupplierId(), p.getStatus(),
                p.getExpectedDate(), p.getRemark(), p.getCreatedAt(), p.getUpdatedAt()
        );
        return ApiResponse.ok(vo);
    }

    @PostMapping("/{id}/receive-all")
    public ApiResponse<Void> receiveAll(@PathVariable Long id) {
        purchaseOrderService.receiveAll(id);
        return ApiResponse.ok();
    }
}

