package com.example.backend.module.base.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BomBulkSetRequest(
        @NotNull Long productMaterialId,
        @NotNull List<BomLineRequest> lines
) {
}

