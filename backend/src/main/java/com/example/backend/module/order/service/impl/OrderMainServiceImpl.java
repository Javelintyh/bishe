package com.example.backend.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.common.exception.ApiException;
import com.example.backend.module.order.dto.CreateOrderRequest;
import com.example.backend.module.order.entity.OrderDetail;
import com.example.backend.module.order.entity.OrderMain;
import com.example.backend.module.order.mapper.OrderMainMapper;
import com.example.backend.module.order.service.OrderDetailService;
import com.example.backend.module.order.service.OrderMainService;
import com.example.backend.module.production.entity.ProductionWorkOrder;
import com.example.backend.module.production.entity.ProductionWorkOrderProcess;
import com.example.backend.module.production.service.ProductionWorkOrderProcessService;
import com.example.backend.module.production.service.ProductionWorkOrderService;
import com.example.backend.module.message.entity.MessageNotice;
import com.example.backend.module.message.service.MessageNoticeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class OrderMainServiceImpl extends ServiceImpl<OrderMainMapper, OrderMain> implements OrderMainService {
    private final OrderDetailService orderDetailService;
    private final ProductionWorkOrderService workOrderService;
    private final ProductionWorkOrderProcessService workOrderProcessService;
    private final MessageNoticeService messageNoticeService;

    public OrderMainServiceImpl(
            OrderDetailService orderDetailService,
            ProductionWorkOrderService workOrderService,
            ProductionWorkOrderProcessService workOrderProcessService,
            MessageNoticeService messageNoticeService
    ) {
        this.orderDetailService = orderDetailService;
        this.workOrderService = workOrderService;
        this.workOrderProcessService = workOrderProcessService;
        this.messageNoticeService = messageNoticeService;
    }

    @Override
    @Transactional
    public Long createOrder(CreateOrderRequest req) {
        boolean exists = this.exists(new LambdaQueryWrapper<OrderMain>().eq(OrderMain::getOrderNo, req.orderNo()));
        if (exists) {
            throw new ApiException(400, "订单号已存在");
        }

        OrderMain main = new OrderMain();
        main.setOrderNo(req.orderNo());
        main.setCustomerId(req.customerId());
        main.setStatus("PENDING");
        main.setDeliveryDate(req.deliveryDate());

        // 优先级规则：加急 => 自动置顶（pinned=true）
        boolean urgent = Boolean.TRUE.equals(req.urgent());
        boolean pinned = Boolean.TRUE.equals(req.pinned()) || urgent;
        main.setUrgent(urgent);
        main.setPinned(pinned);
        this.save(main);

        OrderDetail detail = new OrderDetail();
        detail.setOrderId(main.getId());
        detail.setProductMaterialId(req.productMaterialId());
        detail.setQty(req.qty());
        orderDetailService.save(detail);

        ProductionWorkOrder wo = new ProductionWorkOrder();
        wo.setWorkOrderNo(generateWorkOrderNo());
        wo.setOrderId(main.getId());
        wo.setProductMaterialId(req.productMaterialId());
        wo.setQty(req.qty());
        boolean isReplenishOrder = main.getOrderNo() != null && main.getOrderNo().startsWith("RP");
        // 补产单直接进入车间待生产列表（车间端当前按 PRODUCING 展示可报工工单）
        // 同时避免进入仓库“工单待出库”（仓库侧按 TO_PRODUCE 展示）
        wo.setStatus(isReplenishOrder ? "PRODUCING" : "TO_PRODUCE");
        wo.setDueDate(main.getDeliveryDate());
        workOrderService.save(wo);

        ProductionWorkOrderProcess p = new ProductionWorkOrderProcess();
        p.setWorkOrderId(wo.getId());
        p.setProcessName("默认工序");
        p.setSeqNo(1);
        p.setPlannedQty(req.qty());
        workOrderProcessService.save(p);

        if (!isReplenishOrder) {
            // 普通订单：给仓库端发送“工单待出库”提示
            boolean woCreatedMsgExists = messageNoticeService.exists(new LambdaQueryWrapper<MessageNotice>()
                    .eq(MessageNotice::getNoticeType, "WO_CREATED")
                    .eq(MessageNotice::getRelatedType, "WORK_ORDER")
                    .eq(MessageNotice::getRelatedId, wo.getWorkOrderNo())
                    .eq(MessageNotice::getIsRead, false));
            if (!woCreatedMsgExists) {
                MessageNotice n = new MessageNotice();
                n.setNoticeType("WO_CREATED");
                n.setTitle("新增订单");
                String deliveryText = main.getDeliveryDate() != null ? main.getDeliveryDate().toString() : "";
                StringBuilder content = new StringBuilder();
                content.append("工单号: ").append(wo.getWorkOrderNo()).append('\n');
                if (!deliveryText.isBlank()) {
                    content.append("交货期: ").append(deliveryText);
                }
                n.setContent(content.toString());
                n.setLevel("INFO");
                n.setRelatedType("WORK_ORDER");
                n.setRelatedId(wo.getWorkOrderNo());
                n.setIsRead(false);
                n.setCreatedAt(LocalDateTime.now());
                messageNoticeService.save(n);
            }

            // 普通订单加急时，给仓库端额外发送加急提示
            if (Boolean.TRUE.equals(main.isUrgent())) {
                boolean woUrgentMsgExists = messageNoticeService.exists(new LambdaQueryWrapper<MessageNotice>()
                        .eq(MessageNotice::getNoticeType, "WO_URGENT")
                        .eq(MessageNotice::getRelatedType, "WORK_ORDER")
                        .eq(MessageNotice::getRelatedId, wo.getWorkOrderNo())
                        .eq(MessageNotice::getIsRead, false));
                if (!woUrgentMsgExists) {
                    MessageNotice n = new MessageNotice();
                    n.setNoticeType("WO_URGENT");
                    n.setTitle("加急订单待出库");
                    String deliveryText = main.getDeliveryDate() != null ? main.getDeliveryDate().toString() : "";
                    StringBuilder content = new StringBuilder();
                    content.append("工单号: ").append(wo.getWorkOrderNo()).append('\n');
                    if (!deliveryText.isBlank()) {
                        content.append("交货期: ").append(deliveryText);
                    }
                    n.setContent(content.toString());
                    n.setLevel("WARN");
                    n.setRelatedType("WORK_ORDER");
                    n.setRelatedId(wo.getWorkOrderNo());
                    n.setIsRead(false);
                    n.setCreatedAt(LocalDateTime.now());
                    messageNoticeService.save(n);
                }
            }
        }

        return main.getId();
    }

    private static String generateWorkOrderNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        long tail = System.nanoTime() % 10000;
        return "WO" + ts + String.format("%04d", Math.abs(tail));
    }
}

