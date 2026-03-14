package com.example.backend.module.report.dto;

import java.math.BigDecimal;

public record InventoryTurnoverDTO(
        BigDecimal totalInQty,
        BigDecimal totalOutQty,
        BigDecimal currentStockQty
) {
}

