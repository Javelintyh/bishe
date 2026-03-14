package com.example.backend.module.device.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.module.device.entity.DeviceStatusLog;
import com.example.backend.module.device.mapper.DeviceStatusLogMapper;
import com.example.backend.module.device.service.DeviceStatusLogService;
import org.springframework.stereotype.Service;

@Service
public class DeviceStatusLogServiceImpl extends ServiceImpl<DeviceStatusLogMapper, DeviceStatusLog> implements DeviceStatusLogService {
}

