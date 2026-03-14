package com.example.backend.module.quality.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("quality_reason_dict")
public class QualityReasonDict {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("reason_code")
    private String reasonCode;

    @TableField("reason_name")
    private String reasonName;

    private Boolean enabled;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

