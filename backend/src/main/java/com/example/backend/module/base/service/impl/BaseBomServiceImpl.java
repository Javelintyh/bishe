package com.example.backend.module.base.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.module.base.entity.BaseBom;
import com.example.backend.module.base.mapper.BaseBomMapper;
import com.example.backend.module.base.service.BaseBomService;
import org.springframework.stereotype.Service;

@Service
public class BaseBomServiceImpl extends ServiceImpl<BaseBomMapper, BaseBom> implements BaseBomService {
}

