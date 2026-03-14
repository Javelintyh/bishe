package com.example.backend.module.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.api.ApiResponse;
import com.example.backend.module.inventory.entity.InventoryRecord;
import com.example.backend.module.inventory.entity.InventoryStock;
import com.example.backend.module.inventory.mapper.InventoryRecordMapper;
import com.example.backend.module.inventory.mapper.InventoryStockMapper;
import com.example.backend.module.order.dto.OrderVO;
import com.example.backend.module.order.entity.OrderMain;
import com.example.backend.module.order.service.OrderMainService;
import com.example.backend.module.production.entity.ProductionWorkOrder;
import com.example.backend.module.production.service.ProductionWorkOrderService;
import com.example.backend.module.report.dto.InventoryTurnoverDTO;
import com.example.backend.module.report.dto.OrderSummaryDTO;
import com.example.backend.module.report.dto.ProductionSummaryDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/report")
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {
    private final OrderMainService orderMainService;
    private final ProductionWorkOrderService workOrderService;
    private final InventoryRecordMapper inventoryRecordMapper;
    private final InventoryStockMapper inventoryStockMapper;

    public ReportController(
            OrderMainService orderMainService,
            ProductionWorkOrderService workOrderService,
            InventoryRecordMapper inventoryRecordMapper,
            InventoryStockMapper inventoryStockMapper
    ) {
        this.orderMainService = orderMainService;
        this.workOrderService = workOrderService;
        this.inventoryRecordMapper = inventoryRecordMapper;
        this.inventoryStockMapper = inventoryStockMapper;
    }

    @GetMapping("/order-summary")
    public ApiResponse<OrderSummaryDTO> orderSummary() {
        long total = orderMainService.count();
        long delivered = orderMainService.count(new LambdaQueryWrapper<OrderMain>().eq(OrderMain::getStatus, "DELIVERED"));
        long onTime = orderMainService.count(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getStatus, "DELIVERED")
                .isNotNull(OrderMain::getActualDeliveryDate)
                .apply("actual_delivery_date <= delivery_date"));
        return ApiResponse.ok(new OrderSummaryDTO(total, delivered, onTime));
    }

    @GetMapping("/production-summary")
    public ApiResponse<ProductionSummaryDTO> productionSummary() {
        long total = workOrderService.count();
        long done = workOrderService.count(new LambdaQueryWrapper<ProductionWorkOrder>().eq(ProductionWorkOrder::getStatus, "DONE"));
        long producing = workOrderService.count(new LambdaQueryWrapper<ProductionWorkOrder>().eq(ProductionWorkOrder::getStatus, "PRODUCING"));
        return ApiResponse.ok(new ProductionSummaryDTO(total, done, producing));
    }

    @GetMapping("/inventory-turnover")
    public ApiResponse<InventoryTurnoverDTO> inventoryTurnover() {
        LocalDate start = LocalDate.now().minusDays(30);
        List<InventoryRecord> records = inventoryRecordMapper.selectList(
                new LambdaQueryWrapper<InventoryRecord>()
                        .ge(InventoryRecord::getCreatedAt, start.atStartOfDay())
        );
        BigDecimal inQty = records.stream()
                .filter(r -> r.getChangeQty() != null && r.getChangeQty().compareTo(BigDecimal.ZERO) > 0)
                .map(InventoryRecord::getChangeQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outQty = records.stream()
                .filter(r -> r.getChangeQty() != null && r.getChangeQty().compareTo(BigDecimal.ZERO) < 0)
                .map(r -> r.getChangeQty().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal stockQty = inventoryStockMapper.selectList(null).stream()
                .map(InventoryStock::getQty)
                .filter(q -> q != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return ApiResponse.ok(new InventoryTurnoverDTO(inQty, outQty, stockQty));
    }
}

