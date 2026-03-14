package com.example.backend.module.sys.controller;

import com.example.backend.common.api.ApiResponse;
import com.example.backend.common.exception.ApiException;
import com.example.backend.module.sys.dto.ChangePasswordRequest;
import com.example.backend.module.sys.dto.CreateUserRequest;
import com.example.backend.module.sys.dto.UpdateUserRequest;
import com.example.backend.module.sys.dto.UserVO;
import com.example.backend.module.sys.entity.SysUser;
import com.example.backend.module.sys.security.SysUserDetails;
import com.example.backend.module.sys.service.SysUserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sys/users")
public class SysUserController {
    private final SysUserService sysUserService;
    private final PasswordEncoder passwordEncoder;

    public SysUserController(SysUserService sysUserService, PasswordEncoder passwordEncoder) {
        this.sysUserService = sysUserService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserVO>> list() {
        SysUserDetails current = currentUserDetails();
        boolean isSuperAdmin = "admin".equals(current.getUsername());
        List<UserVO> users = sysUserService.list().stream()
                .filter(u -> isSuperAdmin || !"ADMIN".equals(u.getRoleCode()))
                .map(SysUserController::toVO)
                .toList();
        return ApiResponse.ok(users);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserVO> create(@Valid @RequestBody CreateUserRequest req) {
        if (sysUserService.findByUsername(req.username()).isPresent()) {
            throw new ApiException(400, "用户名已存在");
        }

        String rawPassword = (req.password() == null || req.password().isBlank()) ? "123456" : req.password();
        SysUser user = new SysUser();
        user.setUsername(req.username());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRoleCode(req.roleCode());
        user.setEnabled(true);

        sysUserService.save(user);
        return ApiResponse.ok(toVO(user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserVO> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            throw new ApiException(404, "用户不存在");
        }
        SysUserDetails current = currentUserDetails();
        boolean isSuperAdmin = "admin".equals(current.getUsername());
        if ("admin".equals(user.getUsername())) {
            throw new ApiException(400, "不能修改主管理员账号");
        }
        if (!isSuperAdmin && "ADMIN".equals(user.getRoleCode())) {
            throw new ApiException(403, "无权管理管理员账号");
        }
        user.setRoleCode(req.roleCode());
        if (req.enabled() != null) {
            user.setEnabled(req.enabled());
        }
        sysUserService.updateById(user);
        return ApiResponse.ok(toVO(user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            throw new ApiException(404, "用户不存在");
        }
        SysUserDetails current = currentUserDetails();
        boolean isSuperAdmin = "admin".equals(current.getUsername());
        if ("admin".equals(user.getUsername())) {
            throw new ApiException(400, "不能删除主管理员账号");
        }
        if (!isSuperAdmin && "ADMIN".equals(user.getRoleCode())) {
            throw new ApiException(403, "无权管理管理员账号");
        }
        sysUserService.removeById(id);
        return ApiResponse.ok();
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        SysUserDetails userDetails = currentUserDetails();
        SysUser user = userDetails.getUser();

        if (!passwordEncoder.matches(req.oldPassword(), user.getPasswordHash())) {
            throw new ApiException(400, "旧密码不正确");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        sysUserService.updateById(user);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> resetPassword(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            throw new ApiException(404, "用户不存在");
        }
        SysUserDetails current = currentUserDetails();
        boolean isSuperAdmin = "admin".equals(current.getUsername());
        if ("admin".equals(user.getUsername()) && !isSuperAdmin) {
            throw new ApiException(403, "无权重置主管理员密码");
        }
        user.setPasswordHash(passwordEncoder.encode("123456"));
        sysUserService.updateById(user);
        return ApiResponse.ok();
    }

    private static UserVO toVO(SysUser u) {
        return new UserVO(u.getId(), u.getUsername(), u.getRoleCode(), u.getEnabled(), u.getCreatedAt(), u.getUpdatedAt());
    }

    private static SysUserDetails currentUserDetails() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SysUserDetails userDetails)) {
            throw new ApiException(401, "未登录");
        }
        return userDetails;
    }
}

