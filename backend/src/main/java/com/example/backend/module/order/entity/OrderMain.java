package com.example.backend.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("order_main")
public class OrderMain {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("order_no")
    private String orderNo;

    @TableField("customer_id")
    private Long customerId;

    private String status;

    @TableField("delivery_date")
    private LocalDate deliveryDate;

    @TableField("actual_delivery_date")
    private LocalDate actualDeliveryDate;

    /**
     * 加急：用于优先排队生产/出库
     * 置顶：用于更高优先级（排序第 1）
     */
    @TableField("urgent")
    private boolean urgent;

    @TableField("pinned")
    private boolean pinned;

    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

