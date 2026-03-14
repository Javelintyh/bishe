package com.example.backend.module.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateOrderRequest(
        @NotBlank String orderNo,
        @NotNull Long customerId,
        @NotNull Long productMaterialId,
        @NotNull BigDecimal qty,
        LocalDate deliveryDate
) {
}

