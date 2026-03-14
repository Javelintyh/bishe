package com.example.backend.module.production.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateReportRequest(
        @NotBlank String workOrderNo,
        String processName,
        @NotNull BigDecimal goodQty,
        BigDecimal badQty,
        String badReasonCode,
        String badReasonText
) {
}

