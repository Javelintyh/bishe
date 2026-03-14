package com.example.backend.module.sys.security;

import com.example.backend.module.sys.entity.SysUser;
import com.example.backend.module.sys.service.SysUserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class SysUserDetailsService implements UserDetailsService {
    private final SysUserService sysUserService;

    public SysUserDetailsService(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("user not found"));
        return new SysUserDetails(user);
    }
}

