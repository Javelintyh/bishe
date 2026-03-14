package com.example.backend.module.base.dto;

import jakarta.validation.constraints.NotBlank;

public record SupplierCreateRequest(
        @NotBlank String supplierName,
        String contactName,
        String contactPhone,
        String address
) {
}

