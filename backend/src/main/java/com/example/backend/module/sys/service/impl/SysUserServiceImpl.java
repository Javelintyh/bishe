package com.example.backend.module.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.module.sys.entity.SysUser;
import com.example.backend.module.sys.mapper.SysUserMapper;
import com.example.backend.module.sys.service.SysUserService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
    @Override
    public Optional<SysUser> findByUsername(String username) {
        SysUser user = this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("limit 1"));
        return Optional.ofNullable(user);
    }
}

