package com.example.backend.module.base.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.module.base.entity.BaseSupplier;
import com.example.backend.module.base.mapper.BaseSupplierMapper;
import com.example.backend.module.base.service.BaseSupplierService;
import org.springframework.stereotype.Service;

@Service
public class BaseSupplierServiceImpl extends ServiceImpl<BaseSupplierMapper, BaseSupplier> implements BaseSupplierService {
}

