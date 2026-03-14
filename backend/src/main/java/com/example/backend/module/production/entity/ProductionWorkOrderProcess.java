package com.example.backend.module.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("production_work_order_process")
public class ProductionWorkOrderProcess {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("work_order_id")
    private Long workOrderId;

    @TableField("process_name")
    private String processName;

    @TableField("seq_no")
    private Integer seqNo;

    @TableField("planned_qty")
    private BigDecimal plannedQty;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

