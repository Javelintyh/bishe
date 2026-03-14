package com.example.backend.module.base.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.api.ApiResponse;
import com.example.backend.common.exception.ApiException;
import com.example.backend.module.base.dto.BomBulkSetRequest;
import com.example.backend.module.base.dto.BomLineRequest;
import com.example.backend.module.base.entity.BaseBom;
import com.example.backend.module.base.service.BaseBomService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/base/boms")
public class BaseBomController {
    private final BaseBomService baseBomService;

    public BaseBomController(BaseBomService baseBomService) {
        this.baseBomService = baseBomService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WORKSHOP','WAREHOUSE')")
    public ApiResponse<List<BaseBom>> list(@RequestParam Long productMaterialId) {
        List<BaseBom> list = baseBomService.list(new LambdaQueryWrapper<BaseBom>()
                .eq(BaseBom::getProductMaterialId, productMaterialId)
                .orderByDesc(BaseBom::getId));
        return ApiResponse.ok(list);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','WORKSHOP')")
    public ApiResponse<BaseBom> create(@RequestParam Long productMaterialId, @Valid @RequestBody BomLineRequest req) {
        boolean exists = baseBomService.exists(new LambdaQueryWrapper<BaseBom>()
                .eq(BaseBom::getProductMaterialId, productMaterialId)
                .eq(BaseBom::getMaterialId, req.materialId()));
        if (exists) {
            throw new ApiException(400, "该产品BOM已存在该物料");
        }
        BaseBom b = new BaseBom();
        b.setProductMaterialId(productMaterialId);
        b.setMaterialId(req.materialId());
        b.setQty(req.qty());
        b.setRemark(req.remark());
        baseBomService.save(b);
        return ApiResponse.ok(b);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WORKSHOP')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        baseBomService.removeById(id);
        return ApiResponse.ok();
    }

    @PostMapping("/bulk-set")
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','WORKSHOP')")
    public ApiResponse<Void> bulkSet(@Valid @RequestBody BomBulkSetRequest req) {
        Long productId = req.productMaterialId();
        if (req.lines().isEmpty()) {
            baseBomService.remove(new LambdaQueryWrapper<BaseBom>().eq(BaseBom::getProductMaterialId, productId));
            return ApiResponse.ok();
        }

        baseBomService.remove(new LambdaQueryWrapper<BaseBom>().eq(BaseBom::getProductMaterialId, productId));
        List<BaseBom> batch = req.lines().stream().map(line -> {
            BaseBom b = new BaseBom();
            b.setProductMaterialId(productId);
            b.setMaterialId(line.materialId());
            b.setQty(line.qty());
            b.setRemark(line.remark());
            return b;
        }).toList();
        baseBomService.saveBatch(batch);
        return ApiResponse.ok();
    }
}

