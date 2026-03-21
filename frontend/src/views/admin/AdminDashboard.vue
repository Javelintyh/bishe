<script setup lang="ts">
/**
 * 管理端主页面
 *
 * 功能模块：
 * - 总览/报表
 * - 用户管理
 * - 物料、客户、供应商
 * - 订单、工单
 * - 库存、质量
 * - 采购、设备、消息
 */
import { onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import {
  useAdminData,
  useUserOperations,
  usePurchaseForm,
  useMessageOperations,
  useExportReport,
  useAdminDashboardActions,
  useAdminPageActions,
} from '@/composables/admin'
import type { AdminTabName } from '@/types/admin'

import {
  AdminOverview,
  AdminUsersPane,
  AdminMaterialsPane,
  AdminCustomersPane,
  AdminSuppliersPane,
  AdminOrdersPane,
  AdminWorkOrdersPane,
  AdminInventoryPane,
  AdminPurchasePane,
  AdminMessagesPane,
} from '@/components/admin'

const activeTab = ref<AdminTabName>('overview')
const auth = useAuthStore()

const {
  state,
  loadUsers,
  loadMaterials,
  loadCustomers,
  loadSuppliers,
  loadOrders,
  loadWorkOrders,
  loadStocks,
  loadPurchaseOrders,
  loadMessages,
  onOrderRefresh,
  refreshCurrentTab,
  loadAllData,
} = useAdminData()

const { onToggleUserEnabled, onResetPassword, onDeleteUser } = useUserOperations({
  loadUsers,
})

const {
  showPurchaseDialog,
  purchaseForm,
  purchaseFormLine,
  purchaseSubmitting,
  openPurchaseDialog,
  addPurchaseLine,
  removePurchaseLine,
  submitPurchaseOrder,
} = usePurchaseForm({
  loadPurchaseOrders,
})

const ordersPaneRef = ref<any>(null)

const { onScanMessages, onMarkPurchased, onReceivePurchaseFromMessage } = useMessageOperations({
  loadMessages,
  loadPurchaseOrders,
  loadStocks,
})

const { onExportReport } = useExportReport()
const { handleTabClick, handleExportReport, initPage } = useAdminPageActions({
  refreshCurrentTab,
  onExportReport,
  state,
  loadAllData,
})
const {
  messagesReminderCount,
  onWorkOrderRemind,
  closePurchaseDialog,
  onAfterCreateOrderByMessage,
  submitPurchaseOrderFromStockLowMsg,
  onCreatePurchaseFromRequest,
  onCreatePurchaseFromStockLow,
  stockLowActionLabel,
} = useAdminDashboardActions({
  activeTab,
  state,
  loadMessages,
  onOrderRefresh,
  onScanMessages,
  openPurchaseDialog,
  showPurchaseDialog,
  purchaseForm,
  submitPurchaseOrder,
})

onMounted(initPage)
</script>

<template>
  <el-card class="admin-card">
    <template #header>
      <div class="page-header">
        <span class="page-title">管理端</span>
        <span class="page-subtitle">轻量级生产管理平台 · 管理端视图</span>
      </div>
    </template>

    <el-tabs v-model="activeTab" tab-position="left" class="page-tabs" @tab-click="handleTabClick" style="min-height: 420px">
      <el-tab-pane label="总览/报表" name="overview">
        <AdminOverview
          :orders-count="state.orders.length"
          :work-orders-count="state.workOrders.length"
          :materials-count="state.materials.length"
          :order-summary="state.orderSummary"
          :production-summary="state.productionSummary"
          :inventory-turnover="state.inventoryTurnover"
          @export-report="handleExportReport"
        />
      </el-tab-pane>

      <el-tab-pane label="用户管理" name="users">
        <AdminUsersPane
          :users="state.users"
          :on-toggle-user-enabled="onToggleUserEnabled"
          :on-reset-password="onResetPassword"
          :on-delete-user="onDeleteUser"
          :current-username="auth.username"
        />
      </el-tab-pane>

      <el-tab-pane label="物料" name="materials">
        <AdminMaterialsPane :materials="state.materials" @refresh="loadMaterials" />
      </el-tab-pane>

      <el-tab-pane label="客户" name="customers">
        <AdminCustomersPane :customers="state.customers" @refresh="loadCustomers" />
      </el-tab-pane>

      <el-tab-pane label="供应商" name="suppliers">
        <AdminSuppliersPane :suppliers="state.suppliers" @refresh="loadSuppliers" />
      </el-tab-pane>

      <el-tab-pane label="订单" name="orders">
        <AdminOrdersPane
          ref="ordersPaneRef"
          :orders="state.orders"
          :customers="state.customers"
          :materials="state.materials"
          :on-after-create-by-message="onAfterCreateOrderByMessage"
          @refresh="onOrderRefresh"
        />
      </el-tab-pane>

      <el-tab-pane label="工单" name="workOrders">
        <AdminWorkOrdersPane :work-orders="state.workOrders" :orders="state.orders" :materials="state.materials" />
      </el-tab-pane>

      <el-tab-pane label="库存" name="inventory">
        <AdminInventoryPane :stocks="state.stocks" :materials="state.materials" />
      </el-tab-pane>

      <el-tab-pane label="采购" name="purchase">
        <AdminPurchasePane
          :purchase-orders="state.purchaseOrders"
          :suppliers="state.suppliers"
          :materials="state.materials"
          :on-mark-purchased="onMarkPurchased"
          :show-purchase-dialog="showPurchaseDialog"
          :purchase-form="purchaseForm"
          :purchase-form-line="purchaseFormLine"
          :purchase-submitting="purchaseSubmitting"
          :open-purchase-dialog="openPurchaseDialog"
          :add-purchase-line="addPurchaseLine"
          :remove-purchase-line="removePurchaseLine"
          :submit-purchase-order="submitPurchaseOrderFromStockLowMsg"
          :close-purchase-dialog="closePurchaseDialog"
        />
      </el-tab-pane>

      

      <el-tab-pane name="messages">
        <template #label>
          <el-badge
            :value="messagesReminderCount"
            :hidden="messagesReminderCount === 0"
            :max="99"
            class="tab-badge"
          >
            <span>消息</span>
          </el-badge>
        </template>
        <AdminMessagesPane
          :messages="state.messages"
          :on-scan-messages="onScanMessages"
          :on-purchase-inbound="onReceivePurchaseFromMessage"
          :on-create-purchase-from-request="onCreatePurchaseFromRequest"
          :on-create-purchase-from-stock-low="onCreatePurchaseFromStockLow"
          :stock-low-action-label="stockLowActionLabel"
          :on-work-order-remind="onWorkOrderRemind"
        />
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<style scoped>
.admin-card {
  border: 0;
  border-radius: 16px;
  box-shadow: 0 14px 34px rgba(83, 104, 173, 0.18);
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

.action-bar {
  margin-bottom: 8px;
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
  color: #4a7df0;
  background: #eaf1ff;
}

.page-tabs :deep(.el-tabs__active-bar) {
  background-color: #4a7df0;
}

.page-tabs :deep(.el-table) {
  border-radius: 12px;
  overflow: hidden;
}

.page-tabs :deep(.el-table th.el-table__cell) {
  background: #f2f6ff;
}

.page-tabs :deep(.el-button--primary:not(.is-link):not(.is-text)) {
  border: 0;
  background: linear-gradient(90deg, #4a86ff 0%, #7858f7 100%);
}

.page-tabs :deep(.el-button--primary:not(.is-link):not(.is-text):hover) {
  background: linear-gradient(90deg, #3d76df 0%, #6948dd 100%);
}

.page-tabs :deep(.el-button.is-link.el-button--primary) {
  color: #4a7df0;
}

.page-tabs :deep(.el-input__wrapper),
.page-tabs :deep(.el-textarea__inner),
.page-tabs :deep(.el-select__wrapper),
.page-tabs :deep(.el-input-number) {
  border-radius: 10px;
}
</style>
