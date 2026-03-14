package com.example.backend.module.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("production_report")
public class ProductionReport {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("work_order_id")
    private Long workOrderId;

    @TableField("process_name")
    private String processName;

    @TableField("good_qty")
    private BigDecimal goodQty;

    @TableField("bad_qty")
    private BigDecimal badQty;

    @TableField("bad_reason_code")
    private String badReasonCode;

    @TableField("bad_reason_text")
    private String badReasonText;

    @TableField("reporter_user_id")
    private Long reporterUserId;

    @TableField("report_time")
    private LocalDateTime reportTime;
}

