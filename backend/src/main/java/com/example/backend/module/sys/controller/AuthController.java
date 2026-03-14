package com.example.backend.module.sys.controller;

import com.example.backend.common.api.ApiResponse;
import com.example.backend.common.exception.ApiException;
import com.example.backend.module.sys.dto.LoginRequest;
import com.example.backend.module.sys.dto.LoginResponse;
import com.example.backend.module.sys.dto.RegisterRequest;
import com.example.backend.module.sys.entity.AdminInvitation;
import com.example.backend.module.sys.entity.SysUser;
import com.example.backend.module.sys.security.JwtService;
import com.example.backend.module.sys.security.SysUserDetails;
import com.example.backend.module.sys.service.AdminInvitationService;
import com.example.backend.module.sys.service.SysUserService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SysUserService sysUserService;
    private final AdminInvitationService adminInvitationService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            SysUserService sysUserService,
            PasswordEncoder passwordEncoder,
            AdminInvitationService adminInvitationService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.sysUserService = sysUserService;
        this.passwordEncoder = passwordEncoder;
        this.adminInvitationService = adminInvitationService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        var userOpt = sysUserService.findByUsername(req.username());
        if (userOpt.isEmpty()) {
            return ApiResponse.fail(404, "账号不存在");
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password())
            );
            SysUserDetails userDetails = (SysUserDetails) authentication.getPrincipal();
            String roleCode = userDetails.getUser().getRoleCode();
            String token = jwtService.generateToken(userDetails.getUsername(), roleCode);
            return ApiResponse.ok(new LoginResponse(token, userDetails.getUsername(), roleCode, userDetails.getUser().getId()));
        } catch (Exception e) {
            return ApiResponse.fail(400, "密码错误");
        }
    }

    /**
     * 用户自助注册接口，可选择注册端。
     */
    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest req) {
        String roleCode = req.roleCode();
        if (roleCode == null || roleCode.isBlank()) {
            roleCode = "WORKSHOP";
        }
        if (!roleCode.equals("ADMIN") && !roleCode.equals("WORKSHOP") && !roleCode.equals("WAREHOUSE")) {
            throw new ApiException(400, "注册端类型不合法");
        }
        // 同一端下用户名不可重复
        boolean existsInSameRole = sysUserService.lambdaQuery()
                .eq(com.example.backend.module.sys.entity.SysUser::getUsername, req.username())
                .eq(com.example.backend.module.sys.entity.SysUser::getRoleCode, roleCode)
                .one() != null;
        if (existsInSameRole) {
            throw new ApiException(400, "该端下用户名已存在");
        }
        if (roleCode.equals("ADMIN")) {
            String invitation = req.invitationCode();
            if (invitation == null || invitation.isBlank()) {
                throw new ApiException(400, "管理员注册需要邀请码");
            }
            AdminInvitation inv = adminInvitationService.findValidByCode(invitation)
                    .orElseThrow(() -> new ApiException(400, "邀请码无效或已使用"));
            inv.setUsed(true);
            adminInvitationService.updateById(inv);
        }
        SysUser user = new SysUser();
        user.setUsername(req.username());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRoleCode(roleCode);
        user.setEnabled(true);
        sysUserService.save(user);
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<LoginResponse> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SysUserDetails userDetails)) {
            return ApiResponse.fail(401, "未登录");
        }
        String roleCode = userDetails.getUser().getRoleCode();
        return ApiResponse.ok(new LoginResponse(null, userDetails.getUsername(), roleCode, userDetails.getUser().getId()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok();
    }
}

