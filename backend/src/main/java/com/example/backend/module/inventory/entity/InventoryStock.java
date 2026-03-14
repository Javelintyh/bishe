package com.example.backend.module.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inventory_stock")
public class InventoryStock {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("material_id")
    private Long materialId;

    private BigDecimal qty;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

