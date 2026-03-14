package com.example.backend.module.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import java.time.LocalDateTime;

@Service
public class InventoryService {
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
    }

    private void checkAndNotifyLowStock(InventoryStock stock) {
        BaseMaterial material = baseMaterialService.getById(stock.getMaterialId());
        if (material == null || material.getSafetyStock() == null) {
            return;
        }
        if (stock.getQty().compareTo(material.getSafetyStock()) >= 0) {
            return;
        }

        String relatedId = material.getMaterialCode();
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
}

