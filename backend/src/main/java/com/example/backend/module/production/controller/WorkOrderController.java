package com.example.backend.module.production.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.api.ApiResponse;
import com.example.backend.common.exception.ApiException;
import com.example.backend.module.base.entity.BaseBom;
import com.example.backend.module.base.service.BaseBomService;
import com.example.backend.module.inventory.entity.InventoryStock;
import com.example.backend.module.inventory.mapper.InventoryStockMapper;
import com.example.backend.module.production.entity.ProductionWorkOrder;
import com.example.backend.module.production.service.ProductionWorkOrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {
    private final ProductionWorkOrderService workOrderService;
    private final BaseBomService baseBomService;
    private final InventoryStockMapper inventoryStockMapper;

    public WorkOrderController(
            ProductionWorkOrderService workOrderService,
            BaseBomService baseBomService,
            InventoryStockMapper inventoryStockMapper
    ) {
        this.workOrderService = workOrderService;
        this.baseBomService = baseBomService;
        this.inventoryStockMapper = inventoryStockMapper;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WORKSHOP','WAREHOUSE')")
    public ApiResponse<List<ProductionWorkOrder>> list(@RequestParam(required = false) String status) {
        LambdaQueryWrapper<ProductionWorkOrder> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            qw.eq(ProductionWorkOrder::getStatus, status);
        }
        qw.orderByDesc(ProductionWorkOrder::getId);
        return ApiResponse.ok(workOrderService.list(qw));
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

