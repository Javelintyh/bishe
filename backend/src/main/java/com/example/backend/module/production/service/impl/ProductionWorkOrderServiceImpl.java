package com.example.backend.module.production.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.module.production.entity.ProductionWorkOrder;
import com.example.backend.module.production.mapper.ProductionWorkOrderMapper;
import com.example.backend.module.production.service.ProductionWorkOrderService;
import org.springframework.stereotype.Service;

@Service
public class ProductionWorkOrderServiceImpl extends ServiceImpl<ProductionWorkOrderMapper, ProductionWorkOrder> implements ProductionWorkOrderService {
}

