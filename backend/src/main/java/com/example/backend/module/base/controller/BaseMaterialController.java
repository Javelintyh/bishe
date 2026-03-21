package com.example.backend.module.base.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.api.ApiResponse;
import com.example.backend.common.exception.ApiException;
import com.example.backend.module.base.dto.MaterialCreateRequest;
import com.example.backend.module.base.dto.MaterialUpdateRequest;
import com.example.backend.module.base.entity.BaseSupplier;
import com.example.backend.module.base.entity.BaseMaterial;
import com.example.backend.module.base.service.BaseMaterialService;
import com.example.backend.module.base.service.BaseSupplierRawMaterialService;
import com.example.backend.module.base.service.BaseSupplierService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/base/materials")
@PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE','WORKSHOP')")
public class BaseMaterialController {
    private final BaseMaterialService baseMaterialService;
    private final BaseSupplierRawMaterialService baseSupplierRawMaterialService;
    private final BaseSupplierService baseSupplierService;

    public BaseMaterialController(
            BaseMaterialService baseMaterialService,
            BaseSupplierRawMaterialService baseSupplierRawMaterialService,
            BaseSupplierService baseSupplierService
    ) {
        this.baseMaterialService = baseMaterialService;
        this.baseSupplierRawMaterialService = baseSupplierRawMaterialService;
        this.baseSupplierService = baseSupplierService;
    }

    @GetMapping
    public ApiResponse<List<BaseMaterial>> list(
            @RequestParam(required = false) String materialType,
            @RequestParam(required = false) String keyword
    ) {
        LambdaQueryWrapper<BaseMaterial> qw = new LambdaQueryWrapper<>();
        if (materialType != null && !materialType.isBlank()) {
            qw.eq(BaseMaterial::getMaterialType, materialType);
        }
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(BaseMaterial::getMaterialCode, keyword).or().like(BaseMaterial::getMaterialName, keyword));
        }
        qw.orderByDesc(BaseMaterial::getId);
        return ApiResponse.ok(baseMaterialService.list(qw));
    }

    @PostMapping
    public ApiResponse<BaseMaterial> create(@Valid @RequestBody MaterialCreateRequest req) {
        boolean exists = baseMaterialService.exists(
                new LambdaQueryWrapper<BaseMaterial>().eq(BaseMaterial::getMaterialCode, req.materialCode())
        );
        if (exists) {
            throw new ApiException(400, "物料编码已存在");
        }
        BaseMaterial m = new BaseMaterial();
        m.setMaterialCode(req.materialCode());
        m.setMaterialName(req.materialName());
        m.setMaterialSpec(req.materialSpec());
        m.setUnit(req.unit());
        m.setMaterialType(req.materialType());
        m.setSafetyStock(req.safetyStock());
        m.setEnabled(true);
        baseMaterialService.save(m);
        return ApiResponse.ok(m);
    }

    @PutMapping("/{id}")
    public ApiResponse<BaseMaterial> update(@PathVariable Long id, @Valid @RequestBody MaterialUpdateRequest req) {
        BaseMaterial m = baseMaterialService.getById(id);
        if (m == null) {
            throw new ApiException(404, "物料不存在");
        }
        m.setMaterialName(req.materialName());
        m.setMaterialSpec(req.materialSpec());
        m.setUnit(req.unit());
        m.setMaterialType(req.materialType());
        m.setSafetyStock(req.safetyStock());
        m.setEnabled(req.enabled());
        baseMaterialService.updateById(m);
        return ApiResponse.ok(m);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        baseMaterialService.removeById(id);
        return ApiResponse.ok();
    }

    /**
     * 获取包含指定原材料的供应商
     */
    @GetMapping("/{materialId}/suppliers")
    public ApiResponse<List<BaseSupplier>> listSuppliersByRawMaterial(@PathVariable Long materialId) {
        Set<Long> supplierIds = baseSupplierRawMaterialService.list(
                        new LambdaQueryWrapper<com.example.backend.module.base.entity.BaseSupplierRawMaterial>()
                                .eq(com.example.backend.module.base.entity.BaseSupplierRawMaterial::getMaterialId, materialId))
                .stream()
                .map(com.example.backend.module.base.entity.BaseSupplierRawMaterial::getSupplierId)
                .collect(Collectors.toSet());

        if (supplierIds.isEmpty()) {
            return ApiResponse.ok(List.of());
        }

        LambdaQueryWrapper<BaseSupplier> qw = new LambdaQueryWrapper<>();
        qw.in(BaseSupplier::getId, supplierIds).eq(BaseSupplier::getEnabled, true).orderByDesc(BaseSupplier::getId);
        return ApiResponse.ok(baseSupplierService.list(qw));
    }
}

