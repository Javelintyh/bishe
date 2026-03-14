package com.example.backend.module.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_admin_invitation")
public class AdminInvitation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private Boolean used;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

