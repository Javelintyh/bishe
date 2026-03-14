package com.example.backend.module.quality.dto;

import jakarta.validation.constraints.NotBlank;

public record QualityReasonCreateRequest(
        @NotBlank String reasonCode,
        @NotBlank String reasonName,
        Integer sortNo
) {
}

