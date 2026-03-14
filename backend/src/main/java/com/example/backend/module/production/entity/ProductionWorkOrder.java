package com.example.backend.module.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("production_work_order")
public class ProductionWorkOrder {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("work_order_no")
    private String workOrderNo;

    @TableField("order_id")
    private Long orderId;

    @TableField("product_material_id")
    private Long productMaterialId;

    private BigDecimal qty;

    private String status;

    @TableField("due_date")
    private LocalDate dueDate;

    @TableField("assignee_user_id")
    private Long assigneeUserId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

