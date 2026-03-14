package com.example.backend.module.sys.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank String username,
        String password,
        @NotBlank String roleCode
) {
}

