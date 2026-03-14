package com.example.backend.module.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.module.sys.entity.AdminInvitation;

import java.util.Optional;

public interface AdminInvitationService extends IService<AdminInvitation> {

    Optional<AdminInvitation> findValidByCode(String code);

    AdminInvitation getLatest();
}

