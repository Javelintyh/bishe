<script setup lang="ts">
/**
 * 仓库端看板主页面
 * 
 * 功能模块：
 * - 库存总览
 * - 今日出入库记录
 * - 工单待出库
 * - 成品待入库
 * - 待发货
 * - 成品管理
 * - 原材料管理
 * - 采购消息（仅采购相关）
 */
import { onMounted, ref } from 'vue'
import { useWarehouseData, useInventoryOperations, useWarehouseDashboardActions, useWarehousePageActions } from '@/composables/warehouse'
import {
  StockOverviewTable,
  TodayRecordsTable,
  WorkOrderOutboundTable,
  PendingInboundTable,
  PendingShipTable,
  ProductManagementPane,
  RawMaterialPane,
  WarehousePurchaseMessagesPane,
} from '@/components/warehouse'
import type { WarehouseTabName } from '@/types/warehouse'

const activeTab = ref<WarehouseTabName>('stocks')

const {
  loading,
  state,
  loadData,
  materialMap,
  rawMaterials,
  productMaterials,
  pendingOutboundWorkOrders,
  pendingInboundWorkOrders,
  pendingShipOrders,
  todayRecords,
  getStockQty,
  getMaterialInfo,
  getMaterialName,
  getWorkOrderBomItems,
  isWorkOrderStockSufficient,
  getWorkOrderStockStatus,
} = useWarehouseData()

const {
  inboundForm,
  salesOutboundForm,
  purchaseInboundForm,
  outboundForm,
  onSelectInboundWorkOrder,
  onSelectOutboundWorkOrder,
  onQuickOutbound,
  onQuickInbound,
  onShipOrder,
  onInbound,
  onSalesOutbound,
  onRequestPurchase,
  onOutbound,
} = useInventoryOperations({
  loadData,
  getStockQty,
  getMaterialName,
  getWorkOrderBomItems,
  pendingOutboundWorkOrders,
  pendingInboundWorkOrders,
  getAllWorkOrders: () => state.allWorkOrders,
  getOrders: () => state.orders,
})

const {
  purchaseMessagesRefreshToken,
  popupCapacityWarningsAndMaybeJump,
  onWarehouseInboundFromMessage,
  onQuickOutboundFromWorkOrderMessage,
  onCreateProductionFromStockLowMessage,
  onHandleWarehouseInboundUrge,
  handleCapacityWarningJump,
  handleSalesOutboundAndRefreshMessages,
  handleRawOutboundAndRefreshMessages,
  handlePurchaseInbound,
} = useWarehouseDashboardActions({
  activeTab,
  state,
  pendingOutboundWorkOrders,
  productMaterials,
  rawMaterials,
  getStockQty,
  isWorkOrderStockSufficient,
  loadData,
  onRequestPurchase,
  onSalesOutbound,
  onOutbound,
  purchaseInboundForm,
  outboundForm,
  salesOutboundForm,
})

const { refreshAll, initPage } = useWarehousePageActions({
  loadData,
  popupCapacityWarningsAndMaybeJump,
  incrementMessageRefresh: () => {
    purchaseMessagesRefreshToken.value++
  },
})

onMounted(initPage)
</script>

<template>
  <div class="warehouse-dashboard">
    <el-card class="warehouse-card">
      <template #header>
        <div class="page-header">
          <div>
            <div class="page-title">仓库管理</div>
            <div class="page-subtitle">库存出入库、发货管理</div>
          </div>
          <el-button
            class="soft-btn"
            type="primary"
            :loading="loading"
            @click="refreshAll"
          >
            刷新数据
          </el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab" tab-position="left" class="page-tabs" style="min-height: 420px">
        <!-- 库存总览 -->
        <el-tab-pane label="库存总览" name="stocks">
          <StockOverviewTable
            :stocks="state.stocks"
            :material-map="materialMap"
            :loading="loading"
          />
        </el-tab-pane>

        <!-- 今日出入库记录 -->
        <el-tab-pane label="今日出入库记录" name="todayRecords">
          <TodayRecordsTable
            :records="todayRecords"
            :material-map="materialMap"
            :loading="loading"
          />
        </el-tab-pane>

        <!-- 工单待出库 -->
        <el-tab-pane name="workOrders">
          <template #label>
            <el-badge :value="pendingOutboundWorkOrders.length" :hidden="pendingOutboundWorkOrders.length === 0" :max="99" class="tab-badge">
              <span>工单待出库</span>
            </el-badge>
          </template>
          <WorkOrderOutboundTable
            :work-orders="pendingOutboundWorkOrders"
            :material-map="materialMap"
            :loading="loading"
            :get-work-order-bom-items="getWorkOrderBomItems"
            :is-work-order-stock-sufficient="isWorkOrderStockSufficient"
            :get-work-order-stock-status="getWorkOrderStockStatus"
            @quick-outbound="onQuickOutbound"
          />
        </el-tab-pane>

        <!-- 成品待入库 -->
        <el-tab-pane name="pendingInbound">
          <template #label>
            <el-badge :value="pendingInboundWorkOrders.length" :hidden="pendingInboundWorkOrders.length === 0" :max="99" class="tab-badge">
              <span>成品待入库</span>
            </el-badge>
          </template>
          <PendingInboundTable
            :work-orders="pendingInboundWorkOrders"
            :material-map="materialMap"
            :loading="loading"
            @quick-inbound="onQuickInbound"
          />
        </el-tab-pane>

        <!-- 待发货 -->
        <el-tab-pane name="pendingShip">
          <template #label>
            <el-badge :value="pendingShipOrders.length" :hidden="pendingShipOrders.length === 0" :max="99" class="tab-badge">
              <span>待发货</span>
            </el-badge>
          </template>
          <PendingShipTable
            :orders="pendingShipOrders"
            :material-map="materialMap"
            :loading="loading"
            @ship-order="onShipOrder"
          />
        </el-tab-pane>

        <!-- 成品管理 -->
        <el-tab-pane label="成品管理" name="product">
          <ProductManagementPane
            :inbound-form="inboundForm"
            :sales-outbound-form="salesOutboundForm"
            :product-materials="productMaterials"
            :pending-inbound-work-orders="pendingInboundWorkOrders"
            :get-stock-qty="getStockQty"
            :get-material-info="getMaterialInfo"
            @select-inbound-work-order="onSelectInboundWorkOrder"
            @inbound="onInbound"
            @sales-outbound="handleSalesOutboundAndRefreshMessages"
          />
        </el-tab-pane>

        <!-- 原材料管理 -->
        <el-tab-pane label="原材料管理" name="rawMaterial">
          <RawMaterialPane
            :purchase-inbound-form="purchaseInboundForm"
            :outbound-form="outboundForm"
            :raw-materials="rawMaterials"
            :pending-outbound-work-orders="pendingOutboundWorkOrders"
            :get-stock-qty="getStockQty"
            :get-material-info="getMaterialInfo"
            :purchase-button-label="purchaseInboundForm.bizId ? '确认入库' : '申请采购'"
            @select-outbound-work-order="onSelectOutboundWorkOrder"
            @purchase-inbound="handlePurchaseInbound"
            @outbound="handleRawOutboundAndRefreshMessages"
          />
        </el-tab-pane>

        <!-- 消息：采购到货 + 工单临期/超期 -->
        <WarehousePurchaseMessagesPane
          :materials="state.materials"
          :refresh-token="purchaseMessagesRefreshToken"
          :pending-outbound-work-orders="pendingOutboundWorkOrders"
          @inbound-from-message="onWarehouseInboundFromMessage"
          @quick-outbound-from-workorder-message="onQuickOutboundFromWorkOrderMessage"
          @goto-raw-material-outbound="({ msg }) => handleCapacityWarningJump(msg)"
          @create-production-from-stock-low="onCreateProductionFromStockLowMessage"
          @handle-warehouse-inbound-urge="onHandleWarehouseInboundUrge"
        />
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.warehouse-dashboard {
  padding: 20px;
}

.warehouse-card {
  border: 0;
  border-radius: 16px;
  box-shadow: 0 14px 34px rgba(46, 125, 50, 0.15);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-size: 16px;
  font-weight: 700;
  color: #2d3340;
}

.page-subtitle {
  font-size: 12px;
  color: #8f97ab;
}

.soft-btn {
  border-radius: 10px;
}

.tab-badge :deep(.el-badge__content) {
  top: 2px;
  right: -4px;
}

.page-tabs :deep(.el-tabs__item) {
  border-radius: 10px;
  margin: 3px 0;
}

.page-tabs :deep(.el-tabs__item.is-active) {
  color: #2e7d32;
  background: #e8f5e9;
}

.page-tabs :deep(.el-tabs__active-bar) {
  background-color: #2e7d32;
}

.page-tabs :deep(.el-table) {
  border-radius: 12px;
  overflow: hidden;
}

.page-tabs :deep(.el-table th.el-table__cell) {
  background: #e8f5e9;
}

.page-tabs :deep(.el-button--primary:not(.is-link):not(.is-text)) {
  border: 0;
  background: linear-gradient(90deg, #43a047 0%, #2e7d32 100%);
}

.page-tabs :deep(.el-button--primary:not(.is-link):not(.is-text):hover) {
  background: linear-gradient(90deg, #388e3c 0%, #1b5e20 100%);
}

.page-tabs :deep(.el-button.is-link.el-button--primary) {
  color: #2e7d32;
}

.page-tabs :deep(.el-input__wrapper),
.page-tabs :deep(.el-textarea__inner),
.page-tabs :deep(.el-select__wrapper),
.page-tabs :deep(.el-input-number) {
  border-radius: 10px;
}
</style>
