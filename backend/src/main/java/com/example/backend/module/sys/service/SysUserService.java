package com.example.backend.module.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.module.sys.entity.SysUser;

import java.util.Optional;

public interface SysUserService extends IService<SysUser> {
    Optional<SysUser> findByUsername(String username);
}

