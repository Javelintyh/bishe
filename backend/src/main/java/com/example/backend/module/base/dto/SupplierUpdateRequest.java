package com.example.backend.module.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SupplierUpdateRequest(
        @NotBlank String supplierName,
        String contactName,
        String contactPhone,
        String address,
        @NotNull Boolean enabled
) {
}

