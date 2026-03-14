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
import type { TabsPaneContext } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  useAdminData,
  useUserOperations,
  usePurchaseForm,
  useMessageOperations,
  useExportReport,
} from '@/composables/admin'
import { purchaseStatusLabel } from '@/constants/admin'
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
  AdminQualityPane,
  AdminDevicesPane,
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

const { onMarkMessageRead, onScanMessages, onReceivePurchase } = useMessageOperations({
  loadMessages,
  loadPurchaseOrders,
  loadStocks,
})

const { onExportReport } = useExportReport()

function handleTabClick(pane: TabsPaneContext) {
  if (typeof pane.paneName === 'string') {
    refreshCurrentTab(pane.paneName)
  }
}

function handleExportReport() {
  onExportReport(state.orders, state.workOrders, state.stocks)
}

onMounted(loadAllData)
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
        <AdminOrdersPane :orders="state.orders" :customers="state.customers" :materials="state.materials" @refresh="onOrderRefresh" />
      </el-tab-pane>

      <el-tab-pane label="工单" name="workOrders">
        <AdminWorkOrdersPane :work-orders="state.workOrders" :orders="state.orders" :materials="state.materials" />
      </el-tab-pane>

      <el-tab-pane label="库存" name="inventory">
        <AdminInventoryPane :stocks="state.stocks" :materials="state.materials" />
      </el-tab-pane>

      <el-tab-pane label="质量原因" name="quality">
        <AdminQualityPane :reasons="state.reasons" />
      </el-tab-pane>

      <el-tab-pane label="采购" name="purchase">
        <div class="action-bar">
          <el-button size="small" type="primary" @click="openPurchaseDialog">新建采购单</el-button>
        </div>
        <el-table :data="state.purchaseOrders" style="width: 100%" size="small">
          <el-table-column prop="poNo" label="采购单号" />
          <el-table-column prop="supplierId" label="供应商ID" width="120" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              {{ purchaseStatusLabel(row.status) }}
            </template>
          </el-table-column>
          <el-table-column prop="expectedDate" label="预计到货" width="140" />
          <el-table-column prop="remark" label="备注" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button
                v-if="row.status !== 'DONE'"
                link
                type="primary"
                size="small"
                @click="onReceivePurchase(row)"
              >
                全部收货
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-dialog v-model="showPurchaseDialog" title="新建采购单" width="560px" destroy-on-close @close="showPurchaseDialog = false">
          <el-form label-width="100px" @submit.prevent>
            <el-form-item label="采购单号">
              <el-input v-model="purchaseForm.poNo" placeholder="如 PO20250306001" />
            </el-form-item>
            <el-form-item label="供应商">
              <el-select v-model="purchaseForm.supplierId" clearable placeholder="选择供应商" style="width: 100%">
                <el-option v-for="s in state.suppliers" :key="s.id" :label="s.supplierName" :value="s.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="预计到货">
              <el-date-picker
                v-model="purchaseForm.expectedDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择预计到货日期"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="purchaseForm.remark" type="textarea" rows="2" />
            </el-form-item>
            <el-form-item label="明细">
              <div style="width: 100%">
                <el-table :data="purchaseForm.lines" size="small" max-height="200">
                  <el-table-column label="物料" min-width="140">
                    <template #default="{ row }">
                      {{ state.materials.find((m) => m.id === row.materialId)?.materialCode ?? row.materialId }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="qty" label="数量" width="80" />
                  <el-table-column label="操作" width="60">
                    <template #default="{ $index }">
                      <el-button link type="danger" size="small" @click="removePurchaseLine($index)">删除</el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <div style="display: flex; gap: 8px; margin-top: 8px; flex-wrap: wrap; align-items: flex-start">
                  <el-select v-model="purchaseFormLine.materialId" placeholder="物料" clearable style="width: 160px">
                    <el-option v-for="m in state.materials" :key="m.id" :label="`${m.materialCode} ${m.materialName}`" :value="m.id" />
                  </el-select>
                  <el-input-number v-model="purchaseFormLine.qty" :min="1" :step="1" placeholder="数量" style="width: 100px" />
                  <el-button type="primary" size="small" @click="addPurchaseLine">添加行</el-button>
                </div>
              </div>
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="showPurchaseDialog = false">取消</el-button>
            <el-button type="primary" :loading="purchaseSubmitting" @click="submitPurchaseOrder">确定</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <el-tab-pane label="设备" name="devices">
        <AdminDevicesPane :devices="state.devices" />
      </el-tab-pane>

      <el-tab-pane label="消息" name="messages">
        <AdminMessagesPane
          :messages="state.messages"
          :on-scan-messages="onScanMessages"
          :on-mark-message-read="onMarkMessageRead"
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
