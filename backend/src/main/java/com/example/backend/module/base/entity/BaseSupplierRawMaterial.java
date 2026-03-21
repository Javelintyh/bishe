package com.example.backend.module.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 供应商 - 可售原材料映射
 * - 用于实现“选择公司 => 只能选其售卖的原材料”
 * - 用于实现“选择原材料 => 只能选包含此原材料的公司”
 */
@Data
@TableName("base_supplier_raw_material")
public class BaseSupplierRawMaterial {
    @TableField("supplier_id")
    private Long supplierId;

    @TableField("material_id")
    private Long materialId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

