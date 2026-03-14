package com.example.backend.module.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inventory_record")
public class InventoryRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("material_id")
    private Long materialId;

    @TableField("change_qty")
    private BigDecimal changeQty;

    @TableField("biz_type")
    private String bizType;

    @TableField("biz_id")
    private String bizId;

    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

