package com.example.backend.module.sys.dto;

import java.time.LocalDateTime;

public record UserVO(
        Long id,
        String username,
        String roleCode,
        Boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

