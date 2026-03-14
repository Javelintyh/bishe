package com.example.backend.module.report.dto;

public record OrderSummaryDTO(
        long totalCount,
        long deliveredCount,
        long onTimeDeliveredCount
) {
}

