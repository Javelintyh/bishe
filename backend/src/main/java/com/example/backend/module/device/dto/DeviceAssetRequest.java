package com.example.backend.module.device.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceAssetRequest(
        @NotBlank String deviceCode,
        @NotBlank String deviceName,
        String model,
        String location,
        String status,
        String remark
) {
}

