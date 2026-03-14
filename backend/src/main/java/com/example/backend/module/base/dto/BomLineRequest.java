package com.example.backend.module.base.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BomLineRequest(
        @NotNull Long materialId,
        @NotNull BigDecimal qty,
        String remark
) {
}

