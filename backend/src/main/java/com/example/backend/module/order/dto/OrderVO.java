package com.example.backend.module.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record OrderVO(
        Long id,
        String orderNo,
        Long customerId,
        String status,
        LocalDate deliveryDate,
        boolean pinned,
        boolean urgent,
        Long productMaterialId,
        BigDecimal qty,
        String workOrderNo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

