package com.example.backend.module.purchase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("purchase_order_detail")
public class PurchaseOrderDetail {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("po_id")
    private Long poId;

    @TableField("material_id")
    private Long materialId;

    private BigDecimal qty;

    @TableField("received_qty")
    private BigDecimal receivedQty;

    private BigDecimal price;

    private String remark;
}

