package com.example.backend.module.device.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("device_status_log")
public class DeviceStatusLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("device_id")
    private Long deviceId;

    private String status;

    private String remark;

    @TableField("operator_user_id")
    private Long operatorUserId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

