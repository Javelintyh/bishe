package com.example.backend.module.base.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.api.ApiResponse;
import com.example.backend.common.exception.ApiException;
import com.example.backend.module.base.dto.SupplierCreateRequest;
import com.example.backend.module.base.dto.SupplierUpdateRequest;
import com.example.backend.module.base.entity.BaseMaterial;
import com.example.backend.module.base.entity.BaseSupplier;
import com.example.backend.module.base.entity.BaseSupplierRawMaterial;
import com.example.backend.module.base.service.BaseMaterialService;
import com.example.backend.module.base.service.BaseSupplierService;
import com.example.backend.module.base.service.BaseSupplierRawMaterialService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/base/suppliers")
@PreAuthorize("hasRole('ADMIN')")
public class BaseSupplierController {
    private final BaseSupplierService baseSupplierService;
    private final BaseSupplierRawMaterialService baseSupplierRawMaterialService;
    private final BaseMaterialService baseMaterialService;

    public BaseSupplierController(
            BaseSupplierService baseSupplierService,
            BaseSupplierRawMaterialService baseSupplierRawMaterialService,
            BaseMaterialService baseMaterialService
    ) {
        this.baseSupplierService = baseSupplierService;
        this.baseSupplierRawMaterialService = baseSupplierRawMaterialService;
        this.baseMaterialService = baseMaterialService;
    }

    @GetMapping
    public ApiResponse<List<BaseSupplier>> list(@RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<BaseSupplier> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(BaseSupplier::getSupplierName, keyword)
                    .or().like(BaseSupplier::getContactName, keyword)
                    .or().like(BaseSupplier::getContactPhone, keyword));
        }
        qw.orderByDesc(BaseSupplier::getId);
        return ApiResponse.ok(baseSupplierService.list(qw));
    }

    @PostMapping
    public ApiResponse<BaseSupplier> create(@Valid @RequestBody SupplierCreateRequest req) {
        boolean exists = baseSupplierService.exists(
                new LambdaQueryWrapper<BaseSupplier>().eq(BaseSupplier::getSupplierName, req.supplierName())
        );
        if (exists) {
            throw new ApiException(400, "供应商名称已存在");
        }
        BaseSupplier s = new BaseSupplier();
        s.setSupplierName(req.supplierName());
        s.setContactName(req.contactName());
        s.setContactPhone(req.contactPhone());
        s.setAddress(req.address());
        s.setEnabled(true);
        baseSupplierService.save(s);

        // 绑定可售原材料（供应商 - 可售原材料映射）
        if (req.rawMaterialIds() != null && !req.rawMaterialIds().isEmpty()) {
            var supplierId = s.getId();
            if (supplierId != null) {
                var materialIds = req.rawMaterialIds().stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
                if (!materialIds.isEmpty()) {
                    List<BaseSupplierRawMaterial> relations = materialIds.stream()
                            .map(mid -> {
                                BaseSupplierRawMaterial r = new BaseSupplierRawMaterial();
                                r.setSupplierId(supplierId);
                                r.setMaterialId(mid);
                                r.setCreatedAt(LocalDateTime.now());
                                return r;
                            })
                            .toList();
                    baseSupplierRawMaterialService.saveBatch(relations);
                }
            }
        }

        return ApiResponse.ok(s);
    }

    @PutMapping("/{id}")
    public ApiResponse<BaseSupplier> update(@PathVariable Long id, @Valid @RequestBody SupplierUpdateRequest req) {
        BaseSupplier s = baseSupplierService.getById(id);
        if (s == null) {
            throw new ApiException(404, "供应商不存在");
        }
        s.setSupplierName(req.supplierName());
        s.setContactName(req.contactName());
        s.setContactPhone(req.contactPhone());
        s.setAddress(req.address());
        s.setEnabled(req.enabled());
        baseSupplierService.updateById(s);
        return ApiResponse.ok(s);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        baseSupplierService.removeById(id);
        // 同步删除映射关系，避免脏数据
        baseSupplierRawMaterialService.remove(new LambdaQueryWrapper<BaseSupplierRawMaterial>()
                .eq(BaseSupplierRawMaterial::getSupplierId, id));
        return ApiResponse.ok();
    }

    /**
     * 获取指定供应商可售的原材料
     */
    @GetMapping("/{supplierId}/raw-materials")
    public ApiResponse<List<BaseMaterial>> listRawMaterialsBySupplier(@PathVariable Long supplierId) {
        // 从映射表拿到可售原材料 ID
        Set<Long> materialIds = baseSupplierRawMaterialService.list(
                        new LambdaQueryWrapper<com.example.backend.module.base.entity.BaseSupplierRawMaterial>()
                                .eq(com.example.backend.module.base.entity.BaseSupplierRawMaterial::getSupplierId, supplierId))
                .stream()
                .map(com.example.backend.module.base.entity.BaseSupplierRawMaterial::getMaterialId)
                .collect(Collectors.toSet());

        if (materialIds.isEmpty()) {
            return ApiResponse.ok(List.of());
        }

        LambdaQueryWrapper<BaseMaterial> qw = new LambdaQueryWrapper<>();
        qw.in(BaseMaterial::getId, materialIds).eq(BaseMaterial::getMaterialType, "RAW").orderByDesc(BaseMaterial::getId);
        return ApiResponse.ok(baseMaterialService.list(qw));
    }
}

