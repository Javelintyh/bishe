package com.example.backend.module.quality.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.api.ApiResponse;
import com.example.backend.common.exception.ApiException;
import com.example.backend.module.quality.dto.QualityReasonCreateRequest;
import com.example.backend.module.quality.dto.QualityReasonUpdateRequest;
import com.example.backend.module.quality.entity.QualityReasonDict;
import com.example.backend.module.quality.service.QualityReasonDictService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quality/reasons")
public class QualityReasonController {
    private final QualityReasonDictService reasonService;

    public QualityReasonController(QualityReasonDictService reasonService) {
        this.reasonService = reasonService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WORKSHOP')")
    public ApiResponse<List<QualityReasonDict>> list(@RequestParam(required = false) Boolean enabledOnly) {
        LambdaQueryWrapper<QualityReasonDict> qw = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(enabledOnly)) {
            qw.eq(QualityReasonDict::getEnabled, true);
        }
        qw.orderByAsc(QualityReasonDict::getSortNo).orderByDesc(QualityReasonDict::getId);
        return ApiResponse.ok(reasonService.list(qw));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<QualityReasonDict> create(@Valid @RequestBody QualityReasonCreateRequest req) {
        boolean exists = reasonService.exists(new LambdaQueryWrapper<QualityReasonDict>()
                .eq(QualityReasonDict::getReasonCode, req.reasonCode()));
        if (exists) {
            throw new ApiException(400, "原因编码已存在");
        }
        QualityReasonDict r = new QualityReasonDict();
        r.setReasonCode(req.reasonCode());
        r.setReasonName(req.reasonName());
        r.setEnabled(true);
        r.setSortNo(req.sortNo() == null ? 0 : req.sortNo());
        reasonService.save(r);
        return ApiResponse.ok(r);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<QualityReasonDict> update(@PathVariable Long id, @Valid @RequestBody QualityReasonUpdateRequest req) {
        QualityReasonDict r = reasonService.getById(id);
        if (r == null) {
            throw new ApiException(404, "原因不存在");
        }
        r.setReasonName(req.reasonName());
        r.setEnabled(req.enabled());
        r.setSortNo(req.sortNo() == null ? 0 : req.sortNo());
        reasonService.updateById(r);
        return ApiResponse.ok(r);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        reasonService.removeById(id);
        return ApiResponse.ok();
    }
}

