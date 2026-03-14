package com.example.backend.module.base.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.api.ApiResponse;
import com.example.backend.common.exception.ApiException;
import com.example.backend.module.base.dto.SupplierCreateRequest;
import com.example.backend.module.base.dto.SupplierUpdateRequest;
import com.example.backend.module.base.entity.BaseSupplier;
import com.example.backend.module.base.service.BaseSupplierService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/base/suppliers")
@PreAuthorize("hasRole('ADMIN')")
public class BaseSupplierController {
    private final BaseSupplierService baseSupplierService;

    public BaseSupplierController(BaseSupplierService baseSupplierService) {
        this.baseSupplierService = baseSupplierService;
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
        return ApiResponse.ok();
    }
}

