package com.example.backend.module.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record StockChangeRequest(
        @NotNull Long materialId,
        @NotNull BigDecimal qty,
        @NotBlank String bizType,
        String bizId
) {
}

