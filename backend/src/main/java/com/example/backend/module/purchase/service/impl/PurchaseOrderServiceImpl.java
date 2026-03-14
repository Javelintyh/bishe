package com.example.backend.module.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.common.exception.ApiException;
import com.example.backend.module.inventory.service.InventoryService;
import com.example.backend.module.purchase.dto.CreatePurchaseOrderRequest;
import com.example.backend.module.purchase.dto.PurchaseOrderDetailDTO;
import com.example.backend.module.purchase.entity.PurchaseOrder;
import com.example.backend.module.purchase.entity.PurchaseOrderDetail;
import com.example.backend.module.purchase.mapper.PurchaseOrderMapper;
import com.example.backend.module.purchase.service.PurchaseOrderDetailService;
import com.example.backend.module.purchase.service.PurchaseOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PurchaseOrderServiceImpl extends ServiceImpl<PurchaseOrderMapper, PurchaseOrder> implements PurchaseOrderService {
    private final PurchaseOrderDetailService detailService;
    private final InventoryService inventoryService;

    public PurchaseOrderServiceImpl(
            PurchaseOrderDetailService detailService,
            InventoryService inventoryService
    ) {
        this.detailService = detailService;
        this.inventoryService = inventoryService;
    }

    @Override
    @Transactional
    public Long createOrder(CreatePurchaseOrderRequest req) {
        boolean exists = this.exists(new LambdaQueryWrapper<PurchaseOrder>().eq(PurchaseOrder::getPoNo, req.poNo()));
        if (exists) {
            throw new ApiException(400, "采购单号已存在");
        }
        if (req.lines().isEmpty()) {
            throw new ApiException(400, "采购明细不能为空");
        }

        PurchaseOrder po = new PurchaseOrder();
        po.setPoNo(req.poNo());
        po.setSupplierId(req.supplierId());
        po.setExpectedDate(req.expectedDate());
        po.setRemark(req.remark());
        po.setStatus("CREATED");
        this.save(po);

        for (PurchaseOrderDetailDTO line : req.lines()) {
            PurchaseOrderDetail d = new PurchaseOrderDetail();
            d.setPoId(po.getId());
            d.setMaterialId(line.materialId());
            d.setQty(line.qty());
            d.setReceivedQty(BigDecimal.ZERO);
            d.setPrice(line.price());
            d.setRemark(line.remark());
            detailService.save(d);
        }

        return po.getId();
    }

    @Override
    @Transactional
    public void receiveAll(Long id) {
        PurchaseOrder po = this.getById(id);
        if (po == null) {
            throw new ApiException(404, "采购单不存在");
        }
        if ("DONE".equals(po.getStatus())) {
            return;
        }

        var details = detailService.list(new LambdaQueryWrapper<PurchaseOrderDetail>().eq(PurchaseOrderDetail::getPoId, id));
        if (details.isEmpty()) {
            throw new ApiException(400, "采购明细为空");
        }
        for (PurchaseOrderDetail d : details) {
            BigDecimal toReceive = d.getQty().subtract(d.getReceivedQty() == null ? BigDecimal.ZERO : d.getReceivedQty());
            if (toReceive.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            inventoryService.inbound(d.getMaterialId(), toReceive, "PURCHASE_IN", po.getPoNo());
            d.setReceivedQty(d.getQty());
            detailService.updateById(d);
        }

        po.setStatus("DONE");
        po.setUpdatedAt(LocalDateTime.now());
        this.updateById(po);
    }
}

