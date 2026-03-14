package com.example.backend.module.purchase.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PurchaseOrderDetailDTO(
        @NotNull Long materialId,
        @NotNull BigDecimal qty,
        BigDecimal price,
        String remark
) {
}

