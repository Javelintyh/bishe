package com.example.backend.module.base.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerCreateRequest(
        @NotBlank String customerName,
        String contactName,
        String contactPhone
) {
}

