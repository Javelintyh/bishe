package com.example.backend.module.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message_notice")
public class MessageNotice {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("notice_type")
    private String noticeType;

    private String title;

    private String content;

    private String level;

    @TableField("related_type")
    private String relatedType;

    @TableField("related_id")
    private String relatedId;

    @TableField("is_read")
    private Boolean isRead;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

