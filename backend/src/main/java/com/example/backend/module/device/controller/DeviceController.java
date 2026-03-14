package com.example.backend.module.device.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.api.ApiResponse;
import com.example.backend.common.exception.ApiException;
import com.example.backend.module.device.dto.DeviceAssetRequest;
import com.example.backend.module.device.dto.DeviceStatusChangeRequest;
import com.example.backend.module.device.entity.DeviceAsset;
import com.example.backend.module.device.entity.DeviceStatusLog;
import com.example.backend.module.device.service.DeviceAssetService;
import com.example.backend.module.device.service.DeviceStatusLogService;
import com.example.backend.module.sys.security.SysUserDetails;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/devices")
@PreAuthorize("hasRole('ADMIN')")
public class DeviceController {
    private final DeviceAssetService deviceAssetService;
    private final DeviceStatusLogService deviceStatusLogService;

    public DeviceController(DeviceAssetService deviceAssetService, DeviceStatusLogService deviceStatusLogService) {
        this.deviceAssetService = deviceAssetService;
        this.deviceStatusLogService = deviceStatusLogService;
    }

    @GetMapping
    public ApiResponse<List<DeviceAsset>> list() {
        List<DeviceAsset> list = deviceAssetService.list(new LambdaQueryWrapper<DeviceAsset>().orderByDesc(DeviceAsset::getId));
        return ApiResponse.ok(list);
    }

    @PostMapping
    public ApiResponse<DeviceAsset> create(@Valid @RequestBody DeviceAssetRequest req) {
        boolean exists = deviceAssetService.exists(new LambdaQueryWrapper<DeviceAsset>().eq(DeviceAsset::getDeviceCode, req.deviceCode()));
        if (exists) {
            throw new ApiException(400, "设备编码已存在");
        }
        DeviceAsset a = new DeviceAsset();
        a.setDeviceCode(req.deviceCode());
        a.setDeviceName(req.deviceName());
        a.setModel(req.model());
        a.setLocation(req.location());
        a.setStatus(req.status() == null || req.status().isBlank() ? "IDLE" : req.status());
        a.setRemark(req.remark());
        deviceAssetService.save(a);
        return ApiResponse.ok(a);
    }

    @PutMapping("/{id}")
    public ApiResponse<DeviceAsset> update(@PathVariable Long id, @Valid @RequestBody DeviceAssetRequest req) {
        DeviceAsset a = deviceAssetService.getById(id);
        if (a == null) {
            throw new ApiException(404, "设备不存在");
        }
        a.setDeviceName(req.deviceName());
        a.setModel(req.model());
        a.setLocation(req.location());
        a.setRemark(req.remark());
        deviceAssetService.updateById(a);
        return ApiResponse.ok(a);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        deviceAssetService.removeById(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/status")
    public ApiResponse<DeviceAsset> changeStatus(@PathVariable Long id, @Valid @RequestBody DeviceStatusChangeRequest req) {
        DeviceAsset a = deviceAssetService.getById(id);
        if (a == null) {
            throw new ApiException(404, "设备不存在");
        }
        a.setStatus(req.status());
        deviceAssetService.updateById(a);

        DeviceStatusLog log = new DeviceStatusLog();
        log.setDeviceId(a.getId());
        log.setStatus(req.status());
        log.setRemark(req.remark());
        log.setOperatorUserId(currentUserId());
        log.setCreatedAt(LocalDateTime.now());
        deviceStatusLogService.save(log);

        return ApiResponse.ok(a);
    }

    @GetMapping("/{id}/logs")
    public ApiResponse<List<DeviceStatusLog>> logs(@PathVariable Long id) {
        List<DeviceStatusLog> list = deviceStatusLogService.list(
                new LambdaQueryWrapper<DeviceStatusLog>()
                        .eq(DeviceStatusLog::getDeviceId, id)
                        .orderByDesc(DeviceStatusLog::getId)
        );
        return ApiResponse.ok(list);
    }

    private static Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SysUserDetails userDetails)) {
            return null;
        }
        return userDetails.getUser().getId();
    }
}

