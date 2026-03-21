package com.example.backend.module.base.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.module.base.entity.BaseSupplierRawMaterial;
import com.example.backend.module.base.mapper.BaseSupplierRawMaterialMapper;
import com.example.backend.module.base.service.BaseSupplierRawMaterialService;
import org.springframework.stereotype.Service;

@Service
public class BaseSupplierRawMaterialServiceImpl
        extends ServiceImpl<BaseSupplierRawMaterialMapper, BaseSupplierRawMaterial>
        implements BaseSupplierRawMaterialService {
}

