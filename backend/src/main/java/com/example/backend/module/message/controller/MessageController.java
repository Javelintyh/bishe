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
import com.example.backend.module.production.entity.ProductionWorkOrder;
import com.example.backend.module.production.service.ProductionWorkOrderService;
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
    private final ProductionWorkOrderService productionWorkOrderService;

    public MessageController(
            MessageNoticeService messageNoticeService,
            OrderMainService orderMainService,
            InventoryStockMapper inventoryStockMapper,
            BaseMaterialService baseMaterialService,
            ProductionWorkOrderService productionWorkOrderService
    ) {
        this.messageNoticeService = messageNoticeService;
        this.orderMainService = orderMainService;
        this.inventoryStockMapper = inventoryStockMapper;
        this.baseMaterialService = baseMaterialService;
        this.productionWorkOrderService = productionWorkOrderService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE','WORKSHOP')")
    public ApiResponse<List<MessageNotice>> list(@RequestParam(required = false) Boolean unreadOnly) {
        LambdaQueryWrapper<MessageNotice> qw = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(unreadOnly)) {
            qw.eq(MessageNotice::getIsRead, false);
        }
        qw.orderByDesc(MessageNotice::getId);
        return ApiResponse.ok(messageNoticeService.list(qw));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE','WORKSHOP')")
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
        scanInternal(days);
        return ApiResponse.ok();
    }

    /**
     * 供系统内部（如定时任务）调用的扫描入口，不走方法级鉴权。
     */
    public void scanInternal(Integer days) {
        scanOverdueOrders(days);
        scanWorkOrdersDue(days);
        scanLowStock();
    }

    @PostMapping("/purchase-requests")
    @PreAuthorize("hasRole('WAREHOUSE')")
    public ApiResponse<Void> createPurchaseRequest(
            @RequestParam Long materialId,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) String qty
    ) {
        BaseMaterial m = baseMaterialService.getById(materialId);
        String relatedId = m != null ? m.getMaterialCode() : String.valueOf(materialId);
        MessageNotice n = new MessageNotice();
        n.setNoticeType("PURCHASE_REQUEST");
        n.setTitle("原材料采购请求");
        StringBuilder content = new StringBuilder();
        if (m != null) {
            content.append("物料编码: ").append(m.getMaterialCode()).append('\n');
            content.append("物料名称: ").append(m.getMaterialName()).append('\n');
        } else {
            content.append("物料ID: ").append(materialId).append('\n');
        }
        if (qty != null && !qty.isBlank()) {
            content.append("建议采购数量: ").append(qty).append('\n');
        }
        if (remark != null && !remark.isBlank()) {
            content.append("备注: ").append(remark);
        }
        n.setContent(content.toString());
        n.setLevel("WARN");
        n.setRelatedType("MATERIAL");
        n.setRelatedId(relatedId);
        n.setIsRead(false);
        n.setCreatedAt(LocalDateTime.now());
        messageNoticeService.save(n);
        return ApiResponse.ok();
    }

    @PostMapping("/urge-warehouse-inbound")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    public ApiResponse<Void> urgeWarehouseInbound(
            @RequestParam Long materialId,
            @RequestParam String scene
    ) {
        BaseMaterial m = baseMaterialService.getById(materialId);
        if (m == null) {
            return ApiResponse.ok();
        }
        String relatedId = m.getMaterialCode();
        boolean exists = messageNoticeService.exists(new LambdaQueryWrapper<MessageNotice>()
                .eq(MessageNotice::getNoticeType, "WH_INBOUND_URGE")
                .eq(MessageNotice::getRelatedType, "MATERIAL")
                .eq(MessageNotice::getRelatedId, relatedId)
                .eq(MessageNotice::getIsRead, false));
        if (exists) {
            return ApiResponse.ok();
        }

        MessageNotice n = new MessageNotice();
        n.setNoticeType("WH_INBOUND_URGE");
        n.setTitle("催促仓库入库");
        String normalizedScene = "PRODUCT".equalsIgnoreCase(scene) ? "PRODUCT" : "RAW";
        StringBuilder content = new StringBuilder();
        content.append("场景: ").append(normalizedScene).append('\n');
        content.append("物料编码: ").append(m.getMaterialCode()).append('\n');
        content.append("物料名称: ").append(m.getMaterialName()).append('\n');
        content.append("提示: 请尽快完成入库处理");
        n.setContent(content.toString());
        n.setLevel("WARN");
        n.setRelatedType("MATERIAL");
        n.setRelatedId(relatedId);
        n.setIsRead(false);
        n.setCreatedAt(LocalDateTime.now());
        messageNoticeService.save(n);
        return ApiResponse.ok();
    }

    @PostMapping("/urge-workshop-produce")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    public ApiResponse<Void> urgeWorkshopProduce(@RequestParam Long materialId) {
        BaseMaterial m = baseMaterialService.getById(materialId);
        if (m == null) {
            return ApiResponse.ok();
        }
        String relatedId = m.getMaterialCode();
        boolean exists = messageNoticeService.exists(new LambdaQueryWrapper<MessageNotice>()
                .eq(MessageNotice::getNoticeType, "WS_PRODUCE_URGE")
                .eq(MessageNotice::getRelatedType, "MATERIAL")
                .eq(MessageNotice::getRelatedId, relatedId)
                .eq(MessageNotice::getIsRead, false));
        if (exists) {
            return ApiResponse.ok();
        }

        MessageNotice n = new MessageNotice();
        n.setNoticeType("WS_PRODUCE_URGE");
        n.setTitle("催促车间生产");
        StringBuilder content = new StringBuilder();
        content.append("物料编码: ").append(m.getMaterialCode()).append('\n');
        content.append("物料名称: ").append(m.getMaterialName()).append('\n');
        content.append("提示: 成品低库存，请优先安排生产");
        n.setContent(content.toString());
        n.setLevel("WARN");
        n.setRelatedType("MATERIAL");
        n.setRelatedId(relatedId);
        n.setIsRead(false);
        n.setCreatedAt(LocalDateTime.now());
        messageNoticeService.save(n);
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

    /**
     * 扫描工单临期/超期，并生成消息通知
     *
     * - 临期：due_date 在 today ~ today + dueSoonDays（含）
     * - 超期：due_date < today
     */
    private void scanWorkOrdersDue(Integer days) {
        int dueSoonDays = days != null && days > 0 ? days : 3;
        LocalDate today = LocalDate.now();
        LocalDate dueLimit = today.plusDays(dueSoonDays);

        LambdaQueryWrapper<ProductionWorkOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(ProductionWorkOrder::getStatus, "TO_PRODUCE")
                .isNotNull(ProductionWorkOrder::getDueDate)
                .le(ProductionWorkOrder::getDueDate, dueLimit);

        List<ProductionWorkOrder> list = productionWorkOrderService.list(qw);
        for (ProductionWorkOrder wo : list) {
            if (wo.getDueDate() == null || wo.getWorkOrderNo() == null) continue;

            String noticeType;
            String title;
            long deltaDays = today.toEpochDay() - wo.getDueDate().toEpochDay(); // >0 => overdue

            if (deltaDays > 0) {
                noticeType = "WO_OVERDUE";
                title = "工单超期";
            } else {
                noticeType = "WO_DUE_SOON";
                title = "工单临期";
            }

            String relatedId = wo.getWorkOrderNo();
            if (existsNotice(noticeType, "WORK_ORDER", relatedId)) {
                continue;
            }

            MessageNotice n = new MessageNotice();
            n.setNoticeType(noticeType);
            n.setTitle(title);

            StringBuilder content = new StringBuilder();
            content.append("工单号: ").append(wo.getWorkOrderNo()).append('\n');
            content.append("交货期: ").append(wo.getDueDate()).append('\n');

            if ("WO_OVERDUE".equals(noticeType)) {
                content.append("超期天数: ").append(deltaDays).append(" 天");
            } else {
                long leftDays = wo.getDueDate().toEpochDay() - today.toEpochDay();
                content.append("剩余天数: ").append(Math.max(0, leftDays)).append(" 天");
            }

            n.setContent(content.toString());
            n.setLevel("WARN");
            n.setRelatedType("WORK_ORDER");
            n.setRelatedId(relatedId);
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

