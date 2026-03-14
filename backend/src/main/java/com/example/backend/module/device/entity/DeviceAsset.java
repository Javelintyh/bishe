package com.example.backend.module.device.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("device_asset")
public class DeviceAsset {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("device_code")
    private String deviceCode;

    @TableField("device_name")
    private String deviceName;

    private String model;

    private String location;

    private String status;

    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

