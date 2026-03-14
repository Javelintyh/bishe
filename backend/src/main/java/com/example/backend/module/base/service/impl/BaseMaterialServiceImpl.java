package com.example.backend.module.base.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.module.base.entity.BaseMaterial;
import com.example.backend.module.base.mapper.BaseMaterialMapper;
import com.example.backend.module.base.service.BaseMaterialService;
import org.springframework.stereotype.Service;

@Service
public class BaseMaterialServiceImpl extends ServiceImpl<BaseMaterialMapper, BaseMaterial> implements BaseMaterialService {
}

