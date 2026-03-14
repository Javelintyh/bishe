package com.example.backend.module.message.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.api.ApiResponse;
import com.example.backend.module.message.entity.MessageNotice;
import com.example.backend.module.message.service.MessageNoticeService;
import com.example.backend.module.order.entity.OrderMain;
import com.example.backend.module.order.service.OrderMainService;
import com.example.backend.module.inventory.entity.InventoryStock;
import com.example.backend.module.inventory.mapper.InventoryStockMapper;
import com.example.backend.module.base.entity.BaseMaterial;
import com.example.backend.module.base.service.BaseMaterialService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageNoticeService messageNoticeService;
    private final OrderMainService orderMainService;
    private final InventoryStockMapper inventoryStockMapper;
    private final BaseMaterialService baseMaterialService;

    public MessageController(
            MessageNoticeService messageNoticeService,
            OrderMainService orderMainService,
            InventoryStockMapper inventoryStockMapper,
            BaseMaterialService baseMaterialService
    ) {
        this.messageNoticeService = messageNoticeService;
        this.orderMainService = orderMainService;
        this.inventoryStockMapper = inventoryStockMapper;
        this.baseMaterialService = baseMaterialService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    public ApiResponse<List<MessageNotice>> list(@RequestParam(required = false) Boolean unreadOnly) {
        LambdaQueryWrapper<MessageNotice> qw = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(unreadOnly)) {
            qw.eq(MessageNotice::getIsRead, false);
        }
        qw.orderByDesc(MessageNotice::getId);
        return ApiResponse.ok(messageNoticeService.list(qw));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        MessageNotice n = messageNoticeService.getById(id);
        if (n != null && Boolean.FALSE.equals(n.getIsRead())) {
            n.setIsRead(true);
            messageNoticeService.updateById(n);
        }
        return ApiResponse.ok();
    }

    /**
     * Notes:
     * - 触发工单超时与库存预警扫描（可由定时任务调用，也可手工调用）
     *
     * Args:
     * - days (Integer | null): 最近多少天内的订单/工单参与检查（可选）
     *
     * Returns:
     * - (Void)
     */
    @PostMapping("/scan")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> scan(@RequestParam(required = false) Integer days) {
        scanOverdueOrders(days);
        return ApiResponse.ok();
    }

    private void scanOverdueOrders(Integer days) {
        LambdaQueryWrapper<OrderMain> qw = new LambdaQueryWrapper<>();
        qw.eq(OrderMain::getStatus, "PENDING")
                .lt(OrderMain::getDeliveryDate, LocalDate.now());
        if (days != null && days > 0) {
            qw.ge(OrderMain::getDeliveryDate, LocalDate.now().minusDays(days));
        }
        List<OrderMain> list = orderMainService.list(qw);
        for (OrderMain o : list) {
            if (existsNotice("WO_OVERDUE", "ORDER", o.getOrderNo())) {
                continue;
            }
            MessageNotice n = new MessageNotice();
            n.setNoticeType("WO_OVERDUE");
            n.setTitle("订单超期未完成");
            long daysOverdue = LocalDate.now().toEpochDay() - o.getDeliveryDate().toEpochDay();
            StringBuilder content = new StringBuilder();
            content.append("订单号: ").append(o.getOrderNo()).append('\n');
            content.append("交货期: ").append(o.getDeliveryDate()).append('\n');
            content.append("当前状态: ").append(o.getStatus()).append('\n');
            content.append("超期天数: ").append(daysOverdue).append(" 天");
            n.setContent(content.toString());
            n.setLevel("WARN");
            n.setRelatedType("ORDER");
            n.setRelatedId(o.getOrderNo());
            n.setIsRead(false);
            n.setCreatedAt(LocalDateTime.now());
            messageNoticeService.save(n);
        }
    }

    private void scanLowStock() {
        List<BaseMaterial> materials = baseMaterialService.list();
        if (materials.isEmpty()) {
            return;
        }
        List<InventoryStock> stocks = inventoryStockMapper.selectList(null);
        for (BaseMaterial m : materials) {
            if (m.getSafetyStock() == null || m.getSafetyStock().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal qty = stocks.stream()
                    .filter(s -> s.getMaterialId().equals(m.getId()))
                    .map(InventoryStock::getQty)
                    .findFirst()
                    .orElse(BigDecimal.ZERO);
            if (qty.compareTo(m.getSafetyStock()) >= 0) {
                continue;
            }
            String relatedId = m.getMaterialCode();
            if (existsNotice("STOCK_LOW", "MATERIAL", relatedId)) {
                continue;
            }
            MessageNotice n = new MessageNotice();
            n.setNoticeType("STOCK_LOW");
            n.setTitle("库存预警");
            StringBuilder content = new StringBuilder();
            content.append("物料编码: ").append(m.getMaterialCode()).append('\n');
            content.append("物料名称: ").append(m.getMaterialName()).append('\n');
            content.append("当前库存: ").append(qty).append('\n');
            content.append("安全库存: ").append(m.getSafetyStock());
            n.setContent(content.toString());
            n.setLevel("WARN");
            n.setRelatedType("MATERIAL");
            n.setRelatedId(relatedId);
            n.setIsRead(false);
            n.setCreatedAt(LocalDateTime.now());
            messageNoticeService.save(n);
        }
    }

    private boolean existsNotice(String noticeType, String relatedType, String relatedId) {
        return messageNoticeService.exists(new LambdaQueryWrapper<MessageNotice>()
                .eq(MessageNotice::getNoticeType, noticeType)
                .eq(MessageNotice::getRelatedType, relatedType)
                .eq(MessageNotice::getRelatedId, relatedId)
                .eq(MessageNotice::getIsRead, false));
    }
}

