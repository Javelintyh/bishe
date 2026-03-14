package com.example.backend.module.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MaterialUpdateRequest(
        @NotBlank String materialName,
        String materialSpec,
        String unit,
        @NotBlank String materialType,
        @NotNull BigDecimal safetyStock,
        @NotNull Boolean enabled
) {
}

