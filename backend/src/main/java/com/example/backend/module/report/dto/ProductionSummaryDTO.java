package com.example.backend.module.report.dto;

public record ProductionSummaryDTO(
        long totalWorkOrders,
        long doneWorkOrders,
        long producingWorkOrders
) {
}

