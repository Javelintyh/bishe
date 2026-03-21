package com.example.backend.module.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.common.exception.ApiException;
import com.example.backend.module.inventory.service.InventoryService;
import com.example.backend.module.purchase.dto.CreatePurchaseOrderRequest;
import com.example.backend.module.purchase.dto.PurchaseOrderDetailDTO;
import com.example.backend.module.purchase.entity.PurchaseOrder;
import com.example.backend.module.purchase.entity.PurchaseOrderDetail;
import com.example.backend.module.purchase.mapper.PurchaseOrderMapper;
import com.example.backend.module.message.entity.MessageNotice;
import com.example.backend.module.message.service.MessageNoticeService;
import com.example.backend.module.base.entity.BaseSupplierRawMaterial;
import com.example.backend.module.base.entity.BaseMaterial;
import com.example.backend.module.base.service.BaseMaterialService;
import com.example.backend.module.inventory.entity.InventoryStock;
import com.example.backend.module.inventory.mapper.InventoryStockMapper;
import com.example.backend.module.base.service.BaseSupplierRawMaterialService;
import com.example.backend.module.purchase.service.PurchaseOrderDetailService;
import com.example.backend.module.purchase.service.PurchaseOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class PurchaseOrderServiceImpl extends ServiceImpl<PurchaseOrderMapper, PurchaseOrder> implements PurchaseOrderService {
    private static final BigDecimal MAX_RAW_PER_MATERIAL = new BigDecimal("10000");
    private final PurchaseOrderDetailService detailService;
    private final InventoryService inventoryService;
    private final MessageNoticeService noticeService;
    private final BaseSupplierRawMaterialService supplierRawMaterialService;
    private final BaseMaterialService baseMaterialService;
    private final InventoryStockMapper inventoryStockMapper;

    public PurchaseOrderServiceImpl(
            PurchaseOrderDetailService detailService,
            InventoryService inventoryService,
            MessageNoticeService noticeService,
            BaseSupplierRawMaterialService supplierRawMaterialService,
            BaseMaterialService baseMaterialService,
            InventoryStockMapper inventoryStockMapper
    ) {
        this.detailService = detailService;
        this.inventoryService = inventoryService;
        this.noticeService = noticeService;
        this.supplierRawMaterialService = supplierRawMaterialService;
        this.baseMaterialService = baseMaterialService;
        this.inventoryStockMapper = inventoryStockMapper;
    }

    @Override
    @Transactional
    public Long createOrder(CreatePurchaseOrderRequest req) {
        boolean exists = this.exists(new LambdaQueryWrapper<PurchaseOrder>().eq(PurchaseOrder::getPoNo, req.poNo()));
        if (exists) {
            throw new ApiException(400, "采购单号已存在");
        }
        if (req.lines().isEmpty()) {
            throw new ApiException(400, "采购明细不能为空");
        }
        if (req.supplierId() == null) {
            throw new ApiException(400, "必须选择供应商/公司");
        }

        // 校验：采购明细中每个物料必须属于该供应商可售范围
        Long supplierId = req.supplierId();
        for (PurchaseOrderDetailDTO line : req.lines()) {
            boolean allowed = supplierRawMaterialService.exists(
                    new LambdaQueryWrapper<BaseSupplierRawMaterial>()
                            .eq(BaseSupplierRawMaterial::getSupplierId, supplierId)
                            .eq(BaseSupplierRawMaterial::getMaterialId, line.materialId())
            );
            if (!allowed) {
                throw new ApiException(400, "该供应商不售卖所选物料，无法创建采购单");
            }
        }

        // 校验：创建采购单时，采购数量 + 当前库存 不得超过仓库上限（RAW 按单个物料 10000）
        Map<Long, BigDecimal> requestQtyByMaterial = new HashMap<>();
        for (PurchaseOrderDetailDTO line : req.lines()) {
            if (line.materialId() == null || line.qty() == null) {
                continue;
            }
            requestQtyByMaterial.merge(line.materialId(), line.qty(), BigDecimal::add);
        }
        for (Map.Entry<Long, BigDecimal> entry : requestQtyByMaterial.entrySet()) {
            Long materialId = entry.getKey();
            BigDecimal requestQty = entry.getValue();
            BaseMaterial material = baseMaterialService.getById(materialId);
            if (material == null || !"RAW".equalsIgnoreCase(material.getMaterialType())) {
                continue;
            }

            InventoryStock stock = inventoryStockMapper.selectOne(
                    new LambdaQueryWrapper<InventoryStock>()
                            .eq(InventoryStock::getMaterialId, materialId)
                            .last("limit 1")
            );
            BigDecimal currentQty = (stock == null || stock.getQty() == null) ? BigDecimal.ZERO : stock.getQty();
            BigDecimal maxPurchasable = MAX_RAW_PER_MATERIAL.subtract(currentQty);
            if (maxPurchasable.compareTo(BigDecimal.ZERO) < 0) {
                maxPurchasable = BigDecimal.ZERO;
            }

            if (requestQty.compareTo(maxPurchasable) > 0) {
                String name = material.getMaterialName() == null ? String.valueOf(materialId) : material.getMaterialName();
                throw new ApiException(
                        400,
                        "创建订单失败：原材料【" + name + "】当前库存为 " + currentQty
                                + "，最多可购入 " + maxPurchasable.stripTrailingZeros().toPlainString()
                );
            }
        }

        PurchaseOrder po = new PurchaseOrder();
        po.setPoNo(req.poNo());
        po.setSupplierId(req.supplierId());
        po.setExpectedDate(req.expectedDate());
        po.setRemark(req.remark());
        po.setStatus("CREATED");
        this.save(po);

        for (PurchaseOrderDetailDTO line : req.lines()) {
            PurchaseOrderDetail d = new PurchaseOrderDetail();
            d.setPoId(po.getId());
            d.setMaterialId(line.materialId());
            d.setQty(line.qty());
            d.setReceivedQty(BigDecimal.ZERO);
            d.setPrice(line.price());
            d.setRemark(line.remark());
            detailService.save(d);
        }

        return po.getId();
    }

    @Override
    @Transactional
    public void markPurchased(Long id) {
        PurchaseOrder po = this.getById(id);
        if (po == null) {
            throw new ApiException(404, "采购单不存在");
        }
        if ("DONE".equals(po.getStatus())) {
            throw new ApiException(400, "该采购单已入库");
        }
        if ("RECEIVING".equals(po.getStatus())) {
            // 已经是“已购入”，不重复更新
            return;
        }

        po.setStatus("RECEIVING");
        po.setUpdatedAt(LocalDateTime.now());
        this.updateById(po);

        // 发送“采购已购入”消息，提醒仓库入库
        MessageNotice n = new MessageNotice();
        n.setNoticeType("PURCHASE_ARRIVED");
        n.setTitle("采购单已到货待入库");
        n.setContent("采购单 " + po.getPoNo() + " 已购入，请仓库尽快入库。");
        n.setLevel("INFO");
        n.setRelatedType("PURCHASE_ORDER");
        n.setRelatedId(String.valueOf(po.getId()));
        n.setIsRead(false);
        n.setCreatedAt(LocalDateTime.now());
        noticeService.save(n);
    }

    @Override
    @Transactional
    public void receiveAll(Long id) {
        PurchaseOrder po = this.getById(id);
        if (po == null) {
            throw new ApiException(404, "采购单不存在");
        }
        if ("DONE".equals(po.getStatus())) {
            return;
        }
        if (!"RECEIVING".equals(po.getStatus())) {
            throw new ApiException(400, "请先在管理端执行“购入”操作");
        }

        var details = detailService.list(new LambdaQueryWrapper<PurchaseOrderDetail>().eq(PurchaseOrderDetail::getPoId, id));
        if (details.isEmpty()) {
            throw new ApiException(400, "采购明细为空");
        }
        for (PurchaseOrderDetail d : details) {
            BigDecimal toReceive = d.getQty().subtract(d.getReceivedQty() == null ? BigDecimal.ZERO : d.getReceivedQty());
            if (toReceive.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            inventoryService.inbound(d.getMaterialId(), toReceive, "PURCHASE_IN", po.getPoNo());
            d.setReceivedQty(d.getQty());
            detailService.updateById(d);
        }

        po.setStatus("DONE");
        po.setUpdatedAt(LocalDateTime.now());
        this.updateById(po);
    }
}

