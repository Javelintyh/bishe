package com.example.backend.module.production.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.api.ApiResponse;
import com.example.backend.common.exception.ApiException;
import com.example.backend.module.production.dto.CreateReportRequest;
import com.example.backend.module.production.entity.ProductionReport;
import com.example.backend.module.production.entity.ProductionWorkOrder;
import com.example.backend.module.production.service.ProductionReportService;
import com.example.backend.module.production.service.ProductionWorkOrderService;
import com.example.backend.module.sys.security.SysUserDetails;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ProductionReportController {
    private final ProductionReportService reportService;
    private final ProductionWorkOrderService workOrderService;

    public ProductionReportController(ProductionReportService reportService, ProductionWorkOrderService workOrderService) {
        this.reportService = reportService;
        this.workOrderService = workOrderService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WORKSHOP')")
    public ApiResponse<List<ProductionReport>> list(@RequestParam(required = false) String workOrderNo) {
        LambdaQueryWrapper<ProductionReport> qw = new LambdaQueryWrapper<>();
        if (workOrderNo != null && !workOrderNo.isBlank()) {
            ProductionWorkOrder wo = findWorkOrderByNo(workOrderNo);
            qw.eq(ProductionReport::getWorkOrderId, wo.getId());
        }
        qw.orderByDesc(ProductionReport::getId);
        return ApiResponse.ok(reportService.list(qw));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','WORKSHOP')")
    public ApiResponse<ProductionReport> create(@Valid @RequestBody CreateReportRequest req) {
        ProductionWorkOrder wo = findWorkOrderByNo(req.workOrderNo());

        BigDecimal badQty = req.badQty() == null ? BigDecimal.ZERO : req.badQty();
        if (req.goodQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(400, "完成数量必须大于0");
        }
        if (badQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(400, "不良数量不能为负数");
        }
        if (badQty.compareTo(BigDecimal.ZERO) > 0
                && (req.badReasonCode() == null || req.badReasonCode().isBlank())
                && (req.badReasonText() == null || req.badReasonText().isBlank())) {
            throw new ApiException(400, "不良数量>0时需要填写不良原因");
        }

        SysUserDetails user = currentUserDetails();

        ProductionReport r = new ProductionReport();
        r.setWorkOrderId(wo.getId());
        r.setProcessName(req.processName());
        r.setGoodQty(req.goodQty());
        r.setBadQty(badQty);
        r.setBadReasonCode(req.badReasonCode());
        r.setBadReasonText(req.badReasonText());
        r.setReporterUserId(user.getUser().getId());
        reportService.save(r);

        updateWorkOrderStatusByReports(wo);

        return ApiResponse.ok(r);
    }

    private ProductionWorkOrder findWorkOrderByNo(String workOrderNo) {
        ProductionWorkOrder wo = workOrderService.getOne(new LambdaQueryWrapper<ProductionWorkOrder>()
                .eq(ProductionWorkOrder::getWorkOrderNo, workOrderNo)
                .last("limit 1"));
        if (wo == null) {
            throw new ApiException(404, "工单不存在");
        }
        return wo;
    }

    private void updateWorkOrderStatusByReports(ProductionWorkOrder wo) {
        List<ProductionReport> reports = reportService.list(
                new LambdaQueryWrapper<ProductionReport>().eq(ProductionReport::getWorkOrderId, wo.getId())
        );
        BigDecimal totalGood = reports.stream()
                .map(ProductionReport::getGoodQty)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalGood.compareTo(wo.getQty()) >= 0) {
            wo.setStatus("DONE");
        } else {
            wo.setStatus("PRODUCING");
        }
        workOrderService.updateById(wo);
    }

    private static SysUserDetails currentUserDetails() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SysUserDetails userDetails)) {
            throw new ApiException(401, "未登录");
        }
        return userDetails;
    }
}

