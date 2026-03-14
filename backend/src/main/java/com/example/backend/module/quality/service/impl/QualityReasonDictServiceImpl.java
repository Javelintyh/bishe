package com.example.backend.module.quality.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.module.quality.entity.QualityReasonDict;
import com.example.backend.module.quality.mapper.QualityReasonDictMapper;
import com.example.backend.module.quality.service.QualityReasonDictService;
import org.springframework.stereotype.Service;

@Service
public class QualityReasonDictServiceImpl extends ServiceImpl<QualityReasonDictMapper, QualityReasonDict> implements QualityReasonDictService {
}

