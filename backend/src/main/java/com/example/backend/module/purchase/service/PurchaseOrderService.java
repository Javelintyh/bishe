package com.example.backend.module.purchase.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.module.purchase.dto.CreatePurchaseOrderRequest;
import com.example.backend.module.purchase.entity.PurchaseOrder;

public interface PurchaseOrderService extends IService<PurchaseOrder> {
    Long createOrder(CreatePurchaseOrderRequest req);

    /**
     * 将采购单标记为“已购入”（等待仓库入库）
     */
    void markPurchased(Long id);

    /**
     * 仓库执行“全部入库”
     */
    void receiveAll(Long id);
}

