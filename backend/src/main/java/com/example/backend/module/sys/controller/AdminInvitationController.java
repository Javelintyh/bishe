package com.example.backend.module.sys.controller;

import com.example.backend.common.api.ApiResponse;
import com.example.backend.common.exception.ApiException;
import com.example.backend.module.sys.entity.AdminInvitation;
import com.example.backend.module.sys.security.SysUserDetails;
import com.example.backend.module.sys.service.AdminInvitationService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sys/admin-invitation")
public class AdminInvitationController {

    private final AdminInvitationService adminInvitationService;

    public AdminInvitationController(AdminInvitationService adminInvitationService) {
        this.adminInvitationService = adminInvitationService;
    }

    private static SysUserDetails currentUserDetails() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SysUserDetails userDetails)) {
            throw new ApiException(401, "未登录");
        }
        return userDetails;
    }

    @GetMapping("/current")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> current() {
        SysUserDetails cur = currentUserDetails();
        if (!"admin".equals(cur.getUsername())) {
            throw new ApiException(403, "仅主管理员可查看邀请码");
        }
        AdminInvitation latest = adminInvitationService.getLatest();
        if (latest == null) {
            return ApiResponse.ok(null);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("code", latest.getCode());
        data.put("used", Boolean.TRUE.equals(latest.getUsed()));
        return ApiResponse.ok(data);
    }

    public record UpdateInvitationRequest(@NotBlank(message = "邀请码不能为空") String code) {
    }

    @PostMapping("/update")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> update(@RequestBody UpdateInvitationRequest req) {
        SysUserDetails cur = currentUserDetails();
        if (!"admin".equals(cur.getUsername())) {
            throw new ApiException(403, "仅主管理员可更新邀请码");
        }
        // 先将所有未使用的邀请码标记为已使用
        adminInvitationService.lambdaUpdate()
                .eq(AdminInvitation::getUsed, false)
                .set(AdminInvitation::getUsed, true)
                .update();

        AdminInvitation inv = new AdminInvitation();
        inv.setCode(req.code());
        inv.setUsed(false);
        adminInvitationService.save(inv);

        Map<String, Object> data = new HashMap<>();
        data.put("code", inv.getCode());
        data.put("used", false);
        return ApiResponse.ok(data);
    }
}

