package com.example.backend.module.device.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceStatusChangeRequest(
        @NotBlank String status,
        String remark
) {
}

