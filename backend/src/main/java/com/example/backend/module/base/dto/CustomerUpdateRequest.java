package com.example.backend.module.base.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerUpdateRequest(
        @NotBlank String customerName,
        String contactName,
        String contactPhone
) {
}

