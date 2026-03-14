package com.example.backend.module.message.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.module.message.entity.MessageNotice;
import com.example.backend.module.message.mapper.MessageNoticeMapper;
import com.example.backend.module.message.service.MessageNoticeService;
import org.springframework.stereotype.Service;

@Service
public class MessageNoticeServiceImpl extends ServiceImpl<MessageNoticeMapper, MessageNotice> implements MessageNoticeService {
}

