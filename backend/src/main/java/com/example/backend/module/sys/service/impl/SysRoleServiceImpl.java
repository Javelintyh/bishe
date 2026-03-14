package com.example.backend.module.sys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.module.sys.entity.SysRole;
import com.example.backend.module.sys.mapper.SysRoleMapper;
import com.example.backend.module.sys.service.SysRoleService;
import org.springframework.stereotype.Service;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {
}

