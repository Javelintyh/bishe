package com.example.backend.module.purchase.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.module.purchase.entity.PurchaseOrderDetail;
import com.example.backend.module.purchase.mapper.PurchaseOrderDetailMapper;
import com.example.backend.module.purchase.service.PurchaseOrderDetailService;
import org.springframework.stereotype.Service;

@Service
public class PurchaseOrderDetailServiceImpl
        extends ServiceImpl<PurchaseOrderDetailMapper, PurchaseOrderDetail>
        implements PurchaseOrderDetailService {
}

