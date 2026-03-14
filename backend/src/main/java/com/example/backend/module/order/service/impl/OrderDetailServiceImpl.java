package com.example.backend.module.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.module.order.entity.OrderDetail;
import com.example.backend.module.order.mapper.OrderDetailMapper;
import com.example.backend.module.order.service.OrderDetailService;
import org.springframework.stereotype.Service;

@Service
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper, OrderDetail> implements OrderDetailService {
}

