package com.example.backend.module.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("base_bom")
public class BaseBom {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("product_material_id")
    private Long productMaterialId;

    @TableField("material_id")
    private Long materialId;

    private BigDecimal qty;

    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

