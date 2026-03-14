package com.example.backend.module.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.module.sys.entity.AdminInvitation;
import com.example.backend.module.sys.mapper.AdminInvitationMapper;
import com.example.backend.module.sys.service.AdminInvitationService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AdminInvitationServiceImpl extends ServiceImpl<AdminInvitationMapper, AdminInvitation>
        implements AdminInvitationService {

    @Override
    public Optional<AdminInvitation> findValidByCode(String code) {
        AdminInvitation inv = this.getOne(new LambdaQueryWrapper<AdminInvitation>()
                .eq(AdminInvitation::getCode, code)
                .eq(AdminInvitation::getUsed, false)
                .last("limit 1"));
        return Optional.ofNullable(inv);
    }

    @Override
    public AdminInvitation getLatest() {
        return this.getOne(new LambdaQueryWrapper<AdminInvitation>()
                .orderByDesc(AdminInvitation::getId)
                .last("limit 1"));
    }
}

