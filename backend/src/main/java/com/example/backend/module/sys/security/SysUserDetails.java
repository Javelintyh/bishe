package com.example.backend.module.sys.security;

import com.example.backend.module.sys.entity.SysUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class SysUserDetails implements UserDetails {
    private final SysUser user;
    private final List<GrantedAuthority> authorities;

    public SysUserDetails(SysUser user) {
        this.user = user;
        String roleCode = user.getRoleCode() == null ? "WORKSHOP" : user.getRoleCode();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + roleCode));
    }

    public SysUser getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(user.getEnabled());
    }
}

