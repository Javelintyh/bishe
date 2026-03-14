package com.example.backend.module.sys.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        String username,

        @NotBlank(message = "密码不能为空")
        String password,

        @NotBlank(message = "注册端不能为空")
        String roleCode,

        String invitationCode
) {
}

