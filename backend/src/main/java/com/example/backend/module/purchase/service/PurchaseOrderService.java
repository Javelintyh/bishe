package com.example.backend.module.purchase.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.module.purchase.dto.CreatePurchaseOrderRequest;
import com.example.backend.module.purchase.entity.PurchaseOrder;

public interface PurchaseOrderService extends IService<PurchaseOrder> {
    Long createOrder(CreatePurchaseOrderRequest req);

    void receiveAll(Long id);
}

