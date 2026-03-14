package com.example.backend.module.production.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.module.production.entity.ProductionReport;
import com.example.backend.module.production.mapper.ProductionReportMapper;
import com.example.backend.module.production.service.ProductionReportService;
import org.springframework.stereotype.Service;

@Service
public class ProductionReportServiceImpl extends ServiceImpl<ProductionReportMapper, ProductionReport> implements ProductionReportService {
}

