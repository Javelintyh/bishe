package com.example.backend.module.device.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.module.device.entity.DeviceAsset;
import com.example.backend.module.device.mapper.DeviceAssetMapper;
import com.example.backend.module.device.service.DeviceAssetService;
import org.springframework.stereotype.Service;

@Service
public class DeviceAssetServiceImpl extends ServiceImpl<DeviceAssetMapper, DeviceAsset> implements DeviceAssetService {
}

