package com.example.backend.module.base.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.api.ApiResponse;
import com.example.backend.common.exception.ApiException;
import com.example.backend.module.base.dto.CustomerCreateRequest;
import com.example.backend.module.base.dto.CustomerUpdateRequest;
import com.example.backend.module.base.entity.BaseCustomer;
import com.example.backend.module.base.service.BaseCustomerService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/base/customers")
@PreAuthorize("hasRole('ADMIN')")
public class BaseCustomerController {
    private final BaseCustomerService baseCustomerService;

    public BaseCustomerController(BaseCustomerService baseCustomerService) {
        this.baseCustomerService = baseCustomerService;
    }

    @GetMapping
    public ApiResponse<List<BaseCustomer>> list(@RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<BaseCustomer> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.like(BaseCustomer::getCustomerName, keyword);
        }
        qw.orderByDesc(BaseCustomer::getId);
        return ApiResponse.ok(baseCustomerService.list(qw));
    }

    @PostMapping
    public ApiResponse<BaseCustomer> create(@Valid @RequestBody CustomerCreateRequest req) {
        boolean exists = baseCustomerService.exists(
                new LambdaQueryWrapper<BaseCustomer>().eq(BaseCustomer::getCustomerName, req.customerName())
        );
        if (exists) {
            throw new ApiException(400, "客户名称已存在");
        }
        BaseCustomer c = new BaseCustomer();
        c.setCustomerName(req.customerName());
        c.setContactName(req.contactName());
        c.setContactPhone(req.contactPhone());
        baseCustomerService.save(c);
        return ApiResponse.ok(c);
    }

    @PutMapping("/{id}")
    public ApiResponse<BaseCustomer> update(@PathVariable Long id, @Valid @RequestBody CustomerUpdateRequest req) {
        BaseCustomer c = baseCustomerService.getById(id);
        if (c == null) {
            throw new ApiException(404, "客户不存在");
        }
        c.setCustomerName(req.customerName());
        c.setContactName(req.contactName());
        c.setContactPhone(req.contactPhone());
        baseCustomerService.updateById(c);
        return ApiResponse.ok(c);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        baseCustomerService.removeById(id);
        return ApiResponse.ok();
    }
}

