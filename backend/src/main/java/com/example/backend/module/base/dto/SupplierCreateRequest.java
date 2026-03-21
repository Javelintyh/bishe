package com.example.backend.module.base.dto;

import java.util.List;
import jakarta.validation.constraints.NotBlank;

public record SupplierCreateRequest(
        @NotBlank String supplierName,
        String contactName,
        String contactPhone,
        String address,
        List<Long> rawMaterialIds
) {
}

