package com.example.backend.module.sys.dto;

public record LoginResponse(
        String token,
        String username,
        String roleCode,
        Long userId
) {
}

