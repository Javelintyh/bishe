package com.example.backend.module.production.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.module.production.entity.ProductionWorkOrderProcess;
import com.example.backend.module.production.mapper.ProductionWorkOrderProcessMapper;
import com.example.backend.module.production.service.ProductionWorkOrderProcessService;
import org.springframework.stereotype.Service;

@Service
public class ProductionWorkOrderProcessServiceImpl
        extends ServiceImpl<ProductionWorkOrderProcessMapper, ProductionWorkOrderProcess>
        implements ProductionWorkOrderProcessService {
}

