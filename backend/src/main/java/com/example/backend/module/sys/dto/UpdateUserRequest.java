package com.example.backend.module.sys.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(
        @NotBlank String roleCode,
        Boolean enabled
) {
}

