package com.example.backend.module.purchase.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PurchaseOrderVO(
        Long id,
        String poNo,
        Long supplierId,
        String status,
        LocalDate expectedDate,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

