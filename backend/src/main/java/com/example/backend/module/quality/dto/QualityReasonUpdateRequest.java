package com.example.backend.module.quality.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QualityReasonUpdateRequest(
        @NotBlank String reasonName,
        @NotNull Boolean enabled,
        Integer sortNo
) {
}

