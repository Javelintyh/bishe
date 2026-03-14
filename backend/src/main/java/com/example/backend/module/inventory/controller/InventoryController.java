package com.example.backend.module.inventory.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.api.ApiResponse;
import com.example.backend.module.inventory.dto.StockChangeRequest;
import com.example.backend.module.inventory.entity.InventoryRecord;
import com.example.backend.module.inventory.entity.InventoryStock;
import com.example.backend.module.inventory.mapper.InventoryRecordMapper;
import com.example.backend.module.inventory.mapper.InventoryStockMapper;
import com.example.backend.module.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService inventoryService;
    private final InventoryStockMapper stockMapper;
    private final InventoryRecordMapper recordMapper;

    public InventoryController(InventoryService inventoryService, InventoryStockMapper stockMapper, InventoryRecordMapper recordMapper) {
        this.inventoryService = inventoryService;
        this.stockMapper = stockMapper;
        this.recordMapper = recordMapper;
    }

    @GetMapping("/stocks")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    public ApiResponse<List<InventoryStock>> stocks() {
        return ApiResponse.ok(stockMapper.selectList(new LambdaQueryWrapper<InventoryStock>().orderByDesc(InventoryStock::getId)));
    }

    @PostMapping("/in")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    public ApiResponse<Void> inbound(@Valid @RequestBody StockChangeRequest req) {
        inventoryService.inbound(req.materialId(), req.qty(), req.bizType(), req.bizId());
        return ApiResponse.ok();
    }

    @PostMapping("/out")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    public ApiResponse<Void> outbound(@Valid @RequestBody StockChangeRequest req) {
        inventoryService.outbound(req.materialId(), req.qty(), req.bizType(), req.bizId());
        return ApiResponse.ok();
    }

    @GetMapping("/records")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    public ApiResponse<List<InventoryRecord>> records(
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) String bizType
    ) {
        LambdaQueryWrapper<InventoryRecord> qw = new LambdaQueryWrapper<>();
        if (materialId != null) {
            qw.eq(InventoryRecord::getMaterialId, materialId);
        }
        if (bizType != null && !bizType.isBlank()) {
            qw.eq(InventoryRecord::getBizType, bizType);
        }
        qw.orderByDesc(InventoryRecord::getId);
        return ApiResponse.ok(recordMapper.selectList(qw));
    }
}

