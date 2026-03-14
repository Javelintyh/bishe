package com.example.backend.module.purchase.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreatePurchaseOrderRequest(
        @NotBlank String poNo,
        Long supplierId,
        LocalDate expectedDate,
        String remark,
        @NotNull List<PurchaseOrderDetailDTO> lines
) {
}

