package com.example.backend.module.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.module.order.dto.CreateOrderRequest;
import com.example.backend.module.order.entity.OrderMain;

public interface OrderMainService extends IService<OrderMain> {
    Long createOrder(CreateOrderRequest req);
}

