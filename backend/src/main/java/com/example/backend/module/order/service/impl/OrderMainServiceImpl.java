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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class OrderMainServiceImpl extends ServiceImpl<OrderMainMapper, OrderMain> implements OrderMainService {
    private final OrderDetailService orderDetailService;
    private final ProductionWorkOrderService workOrderService;
    private final ProductionWorkOrderProcessService workOrderProcessService;

    public OrderMainServiceImpl(
            OrderDetailService orderDetailService,
            ProductionWorkOrderService workOrderService,
            ProductionWorkOrderProcessService workOrderProcessService
    ) {
        this.orderDetailService = orderDetailService;
        this.workOrderService = workOrderService;
        this.workOrderProcessService = workOrderProcessService;
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
        wo.setStatus("TO_PRODUCE");
        wo.setDueDate(main.getDeliveryDate());
        workOrderService.save(wo);

        ProductionWorkOrderProcess p = new ProductionWorkOrderProcess();
        p.setWorkOrderId(wo.getId());
        p.setProcessName("默认工序");
        p.setSeqNo(1);
        p.setPlannedQty(req.qty());
        workOrderProcessService.save(p);

        return main.getId();
    }

    private static String generateWorkOrderNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        long tail = System.nanoTime() % 10000;
        return "WO" + ts + String.format("%04d", Math.abs(tail));
    }
}

