package com.example.backend.module.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.backend.common.exception.ApiException;
import com.example.backend.module.base.entity.BaseMaterial;
import com.example.backend.module.base.service.BaseMaterialService;
import com.example.backend.module.inventory.entity.InventoryRecord;
import com.example.backend.module.inventory.entity.InventoryStock;
import com.example.backend.module.inventory.mapper.InventoryRecordMapper;
import com.example.backend.module.inventory.mapper.InventoryStockMapper;
import com.example.backend.module.message.entity.MessageNotice;
import com.example.backend.module.message.service.MessageNoticeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@Service
public class InventoryService {
    // 仓库容量限制
    // - 成品（PRODUCT）按类型总量不超过 1000
    // - 原材料（RAW）按“单个物料”不超过 10000
    private static final BigDecimal MAX_PRODUCT_TOTAL = new BigDecimal("1000");
    private static final BigDecimal MAX_RAW_TOTAL = new BigDecimal("10000");
    private static final BigDecimal PRODUCT_CAPACITY_WARN_REMAINING = new BigDecimal("100");
    private static final BigDecimal RAW_CAPACITY_WARN_REMAINING = new BigDecimal("1000");

    private final InventoryStockMapper stockMapper;
    private final InventoryRecordMapper recordMapper;
    private final BaseMaterialService baseMaterialService;
    private final MessageNoticeService messageNoticeService;

    public InventoryService(
            InventoryStockMapper stockMapper,
            InventoryRecordMapper recordMapper,
            BaseMaterialService baseMaterialService,
            MessageNoticeService messageNoticeService
    ) {
        this.stockMapper = stockMapper;
        this.recordMapper = recordMapper;
        this.baseMaterialService = baseMaterialService;
        this.messageNoticeService = messageNoticeService;
    }

    @Transactional
    public void inbound(Long materialId, BigDecimal qty, String bizType, String bizId) {
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(400, "入库数量必须大于0");
        }
        adjust(materialId, qty, bizType, bizId);
    }

    @Transactional
    public void outbound(Long materialId, BigDecimal qty, String bizType, String bizId) {
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(400, "出库数量必须大于0");
        }
        adjust(materialId, qty.negate(), bizType, bizId);
    }

    private void adjust(Long materialId, BigDecimal changeQty, String bizType, String bizId) {
        if (changeQty.compareTo(BigDecimal.ZERO) > 0) {
            checkWarehouseCapacity(materialId, changeQty);
        }

        InventoryStock stock = stockMapper.selectOne(
                new LambdaQueryWrapper<InventoryStock>().eq(InventoryStock::getMaterialId, materialId).last("limit 1")
        );
        if (stock == null) {
            stock = new InventoryStock();
            stock.setMaterialId(materialId);
            stock.setQty(BigDecimal.ZERO);
            stock.setUpdatedAt(LocalDateTime.now());
            stockMapper.insert(stock);
        }

        BigDecimal next = stock.getQty().add(changeQty);
        if (next.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(400, "库存不足");
        }
        stock.setQty(next);
        stock.setUpdatedAt(LocalDateTime.now());
        stockMapper.updateById(stock);

        InventoryRecord record = new InventoryRecord();
        record.setMaterialId(materialId);
        record.setChangeQty(changeQty);
        record.setBizType(bizType);
        record.setBizId(bizId);
        recordMapper.insert(record);

        // 自动库存预警：当库存跌破安全库存时立即生成消息
        checkAndNotifyLowStock(stock);
        // 自动容量预警：当库存逼近上限时生成提醒，回落后自动解除
        checkAndNotifyCapacityWarning();
    }

    private void checkWarehouseCapacity(Long materialId, BigDecimal inboundQty) {
        BaseMaterial material = baseMaterialService.getById(materialId);
        if (material == null || material.getMaterialType() == null) {
            return;
        }

        String type = material.getMaterialType();
        // 原材料：单个物料上限 10000
        if ("RAW".equalsIgnoreCase(type)) {
            InventoryStock current = stockMapper.selectOne(
                    new LambdaQueryWrapper<InventoryStock>()
                            .eq(InventoryStock::getMaterialId, materialId)
                            .last("limit 1")
            );
            BigDecimal currentQty = (current == null || current.getQty() == null) ? BigDecimal.ZERO : current.getQty();
            BigDecimal nextQty = currentQty.add(inboundQty);
            if (nextQty.compareTo(MAX_RAW_TOTAL) > 0) {
                throw new ApiException(
                        400,
                        ("该原材料库存即将超过上限：当前库存=" + currentQty + " + 本次入库=" + inboundQty + " > 上限=" + MAX_RAW_TOTAL)
                );
            }
            return;
        }

        // 成品：按类型统计总库存量
        BigDecimal max = MAX_PRODUCT_TOTAL;
        List<BaseMaterial> allMaterials = baseMaterialService.list();
        Map<Long, String> typeByMaterialId = new HashMap<>();
        for (BaseMaterial m : allMaterials) {
            if (m.getMaterialType() != null) {
                typeByMaterialId.put(m.getId(), m.getMaterialType());
            }
        }

        List<InventoryStock> stocks = stockMapper.selectList(null);
        BigDecimal currentTotal = BigDecimal.ZERO;
        for (InventoryStock s : stocks) {
            String t = typeByMaterialId.get(s.getMaterialId());
            if (t != null && t.equalsIgnoreCase(type) && s.getQty() != null) {
                currentTotal = currentTotal.add(s.getQty());
            }
        }

        BigDecimal nextTotal = currentTotal.add(inboundQty);
        if (nextTotal.compareTo(max) > 0) {
            throw new ApiException(
                    400,
                    ("该类型仓库库存即将超过上限：当前总量=" + currentTotal + " + 本次入库=" + inboundQty + " > 上限=" + max)
            );
        }
    }

    private void checkAndNotifyLowStock(InventoryStock stock) {
        BaseMaterial material = baseMaterialService.getById(stock.getMaterialId());
        if (material == null || material.getSafetyStock() == null) {
            return;
        }
        String relatedId = material.getMaterialCode();
        if (stock.getQty().compareTo(material.getSafetyStock()) >= 0) {
            // 库存已恢复到安全库存以上：自动解除低库存预警
            resolveUnreadNotice("STOCK_LOW", "MATERIAL", relatedId);
            return;
        }

        boolean exists = messageNoticeService.exists(new LambdaQueryWrapper<MessageNotice>()
                .eq(MessageNotice::getNoticeType, "STOCK_LOW")
                .eq(MessageNotice::getRelatedType, "MATERIAL")
                .eq(MessageNotice::getRelatedId, relatedId)
                .eq(MessageNotice::getIsRead, false));
        if (exists) {
            return;
        }

        MessageNotice n = new MessageNotice();
        n.setNoticeType("STOCK_LOW");
        n.setTitle("库存预警");
        StringBuilder content = new StringBuilder();
        content.append("物料编码: ").append(material.getMaterialCode()).append('\n');
        content.append("物料名称: ").append(material.getMaterialName()).append('\n');
        content.append("当前库存: ").append(stock.getQty()).append('\n');
        content.append("安全库存: ").append(material.getSafetyStock());
        n.setContent(content.toString());
        n.setLevel("WARN");
        n.setRelatedType("MATERIAL");
        n.setRelatedId(relatedId);
        n.setIsRead(false);
        n.setCreatedAt(LocalDateTime.now());
        messageNoticeService.save(n);
    }

    private void checkAndNotifyCapacityWarning() {
        List<BaseMaterial> materials = baseMaterialService.list();
        List<InventoryStock> stocks = stockMapper.selectList(null);
        Map<Long, BigDecimal> qtyMap = new HashMap<>();
        for (InventoryStock s : stocks) {
            qtyMap.put(s.getMaterialId(), s.getQty() == null ? BigDecimal.ZERO : s.getQty());
        }

        // 1) 成品总容量预警：剩余容量 <= 100
        BigDecimal productTotal = BigDecimal.ZERO;
        for (BaseMaterial m : materials) {
            if ("PRODUCT".equalsIgnoreCase(m.getMaterialType())) {
                productTotal = productTotal.add(qtyMap.getOrDefault(m.getId(), BigDecimal.ZERO));
            }
        }
        BigDecimal productRemaining = MAX_PRODUCT_TOTAL.subtract(productTotal);
        if (productRemaining.compareTo(PRODUCT_CAPACITY_WARN_REMAINING) <= 0) {
            if (!existsNotice("CAPACITY_WARN_PRODUCT", "INVENTORY", "PRODUCT_TOTAL")) {
                MessageNotice n = new MessageNotice();
                n.setNoticeType("CAPACITY_WARN_PRODUCT");
                n.setTitle("成品仓容量预警");
                n.setContent(
                        "当前成品总库存: " + productTotal + "\n" +
                        "仓库上限: " + MAX_PRODUCT_TOTAL + "\n" +
                        "剩余容量: " + productRemaining.max(BigDecimal.ZERO)
                );
                n.setLevel("WARN");
                n.setRelatedType("INVENTORY");
                n.setRelatedId("PRODUCT_TOTAL");
                n.setIsRead(false);
                n.setCreatedAt(LocalDateTime.now());
                messageNoticeService.save(n);
            }
        } else {
            resolveUnreadNotice("CAPACITY_WARN_PRODUCT", "INVENTORY", "PRODUCT_TOTAL");
        }

        // 2) 原材料单品容量预警：剩余容量 <= 1000
        for (BaseMaterial m : materials) {
            if (!"RAW".equalsIgnoreCase(m.getMaterialType())) {
                continue;
            }
            BigDecimal qty = qtyMap.getOrDefault(m.getId(), BigDecimal.ZERO);
            BigDecimal remaining = MAX_RAW_TOTAL.subtract(qty);
            String relatedId = m.getMaterialCode();
            if (remaining.compareTo(RAW_CAPACITY_WARN_REMAINING) <= 0) {
                if (!existsNotice("CAPACITY_WARN_RAW", "MATERIAL", relatedId)) {
                    MessageNotice n = new MessageNotice();
                    n.setNoticeType("CAPACITY_WARN_RAW");
                    n.setTitle("原材料仓容量预警");
                    n.setContent(
                            "物料编码: " + m.getMaterialCode() + "\n" +
                            "物料名称: " + m.getMaterialName() + "\n" +
                            "当前库存: " + qty + "\n" +
                            "仓库上限: " + MAX_RAW_TOTAL + "\n" +
                            "剩余容量: " + remaining.max(BigDecimal.ZERO)
                    );
                    n.setLevel("WARN");
                    n.setRelatedType("MATERIAL");
                    n.setRelatedId(relatedId);
                    n.setIsRead(false);
                    n.setCreatedAt(LocalDateTime.now());
                    messageNoticeService.save(n);
                }
            } else {
                resolveUnreadNotice("CAPACITY_WARN_RAW", "MATERIAL", relatedId);
            }
        }
    }

    private boolean existsNotice(String noticeType, String relatedType, String relatedId) {
        return messageNoticeService.exists(new LambdaQueryWrapper<MessageNotice>()
                .eq(MessageNotice::getNoticeType, noticeType)
                .eq(MessageNotice::getRelatedType, relatedType)
                .eq(MessageNotice::getRelatedId, relatedId)
                .eq(MessageNotice::getIsRead, false));
    }

    private void resolveUnreadNotice(String noticeType, String relatedType, String relatedId) {
        MessageNotice updating = new MessageNotice();
        updating.setIsRead(true);
        messageNoticeService.update(
                updating,
                new LambdaUpdateWrapper<MessageNotice>()
                        .eq(MessageNotice::getNoticeType, noticeType)
                        .eq(MessageNotice::getRelatedType, relatedType)
                        .eq(MessageNotice::getRelatedId, relatedId)
                        .eq(MessageNotice::getIsRead, false)
        );
    }
}

