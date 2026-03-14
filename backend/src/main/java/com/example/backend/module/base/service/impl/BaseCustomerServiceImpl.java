package com.example.backend.module.base.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.module.base.entity.BaseCustomer;
import com.example.backend.module.base.mapper.BaseCustomerMapper;
import com.example.backend.module.base.service.BaseCustomerService;
import org.springframework.stereotype.Service;

@Service
public class BaseCustomerServiceImpl extends ServiceImpl<BaseCustomerMapper, BaseCustomer> implements BaseCustomerService {
}

