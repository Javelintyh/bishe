<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TabsPaneContext } from 'element-plus'
import * as sysApi from '@/api/sys'
import * as baseApi from '@/api/base'
import * as orderApi from '@/api/order'
import * as productionApi from '@/api/production'
import * as inventoryApi from '@/api/inventory'
import * as qualityApi from '@/api/quality'
import * as purchaseApi from '@/api/purchase'
import * as deviceApi from '@/api/device'
import * as messageApi from '@/api/message'
import { useAuthStore } from '@/stores/auth'
import * as reportApi from '@/api/report'
import AdminOverview from './AdminOverview.vue'
import AdminUsersPane from './AdminUsersPane.vue'
import AdminMaterialsPane from './AdminMaterialsPane.vue'
import AdminCustomersPane from './AdminCustomersPane.vue'
import AdminSuppliersPane from './AdminSuppliersPane.vue'
import AdminOrdersPane from './AdminOrdersPane.vue'
import AdminWorkOrdersPane from './AdminWorkOrdersPane.vue'
import AdminInventoryPane from './AdminInventoryPane.vue'
import AdminQualityPane from './AdminQualityPane.vue'
import AdminDevicesPane from './AdminDevicesPane.vue'
import AdminMessagesPane from './AdminMessagesPane.vue'
import {
  deviceStatusLabel,
  genPurchaseNo,
  levelLabel,
  materialTypeLabel,
  noticeTypeLabel,
  orderStatusLabel,
  purchaseStatusLabel,
  workOrderStatusLabel,
} from './helpers'

const activeTab = ref('overview')
const showPurchaseDialog = ref(false)
const purchaseForm = reactive({
  poNo: '',
  supplierId: undefined as number | undefined,
  expectedDate: '',
  remark: '',
  lines: [] as { materialId: number; qty: number; price?: number; remark?: string }[],
})
const purchaseFormLine = reactive({ materialId: undefined as number | undefined, qty: 0, price: undefined as number | undefined, remark: '' })
const purchaseSubmitting = ref(false)

const auth = useAuthStore()
const state = reactive({
  users: [] as sysApi.UserVO[],
  materials: [] as baseApi.Material[],
  customers: [] as baseApi.Customer[],
  suppliers: [] as baseApi.Supplier[],
  orders: [] as orderApi.OrderVO[],
  workOrders: [] as productionApi.ProductionWorkOrder[],
  stocks: [] as inventoryApi.InventoryStock[],
  reasons: [] as qualityApi.QualityReason[],
  purchaseOrders: [] as purchaseApi.PurchaseOrder[],
  devices: [] as deviceApi.DeviceAsset[],
  messages: [] as messageApi.MessageNotice[],
  orderSummary: null as reportApi.OrderSummaryDTO | null,
  productionSummary: null as reportApi.ProductionSummaryDTO | null,
  inventoryTurnover: null as reportApi.InventoryTurnoverDTO | null,
})

async function loadUsers() {
  const resp = await sysApi.listUsers()
  if (resp.code === 0) state.users = resp.data ?? []
}

async function loadMaterials() {
  const resp = await baseApi.listMaterials()
  if (resp.code === 0) state.materials = resp.data ?? []
}

async function loadCustomers() {
  const resp = await baseApi.listCustomers()
  if (resp.code === 0) state.customers = resp.data ?? []
}

async function loadSuppliers() {
  const resp = await baseApi.listSuppliers()
  if (resp.code === 0) state.suppliers = resp.data ?? []
}

async function loadOrders() {
  const resp = await orderApi.listOrders()
  if (resp.code === 0) state.orders = resp.data ?? []
}

async function onOrderRefresh() {
  await Promise.all([loadOrders(), loadWorkOrders()])
}

async function loadWorkOrders() {
  const resp = await productionApi.listWorkOrders()
  if (resp.code === 0) state.workOrders = resp.data ?? []
}

async function loadStocks() {
  const resp = await inventoryApi.listStocks()
  if (resp.code === 0) state.stocks = resp.data ?? []
}

async function loadReasons() {
  const resp = await qualityApi.listQualityReasons(true)
  if (resp.code === 0) state.reasons = resp.data ?? []
}

async function loadPurchaseOrders() {
  const resp = await purchaseApi.listPurchaseOrders()
  if (resp.code === 0) state.purchaseOrders = resp.data ?? []
}

async function loadDevices() {
  const resp = await deviceApi.listDevices()
  if (resp.code === 0) state.devices = resp.data ?? []
}

async function loadMessages() {
  const resp = await messageApi.listMessages(true)
  if (resp.code === 0) state.messages = resp.data ?? []
}

async function loadReportSummary() {
  const [o, p, inv] = await Promise.all([
    reportApi.getOrderSummary(),
    reportApi.getProductionSummary(),
    reportApi.getInventoryTurnover(),
  ])
  if (o.code === 0) state.orderSummary = o.data ?? null
  if (p.code === 0) state.productionSummary = p.data ?? null
  if (inv.code === 0) state.inventoryTurnover = inv.data ?? null
}

async function refreshCurrentTab(tabName: string) {
  try {
    if (tabName === 'users') await loadUsers()
    else if (tabName === 'materials') await loadMaterials()
    else if (tabName === 'customers') await loadCustomers()
    else if (tabName === 'suppliers') await loadSuppliers()
    else if (tabName === 'orders') await loadOrders()
    else if (tabName === 'workOrders') await loadWorkOrders()
    else if (tabName === 'inventory') await loadStocks()
    else if (tabName === 'quality') await loadReasons()
    else if (tabName === 'purchase') await loadPurchaseOrders()
    else if (tabName === 'devices') await loadDevices()
    else if (tabName === 'messages') await loadMessages()
    else if (tabName === 'report') await loadReportSummary()
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  }
}

function handleTabClick(pane: TabsPaneContext) {
  if (typeof pane.paneName === 'string') {
    refreshCurrentTab(pane.paneName)
  }
}

async function onToggleUserEnabled(row: sysApi.UserVO) {
  try {
    const nextEnabled = !row.enabled
    await sysApi.updateUser(row.id, { roleCode: row.roleCode, enabled: nextEnabled })
    row.enabled = nextEnabled
    ElMessage.success('已更新用户状态')
  } catch (e: any) {
    ElMessage.error(e?.message || '更新失败')
  }
}

async function onResetPassword(row: sysApi.UserVO) {
  try {
    await ElMessageBox.confirm(`确定将用户 ${row.username} 的密码重置为 123456 吗？`, '提示', {
      type: 'warning',
    })
    await sysApi.resetPassword(row.id)
    ElMessage.success('已重置密码为 123456')
  } catch {
    // ignore cancel
  }
}

async function onDeleteUser(row: sysApi.UserVO) {
  try {
    await ElMessageBox.confirm(`确定删除用户 ${row.username} 吗？此操作不可恢复！`, '警告', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
    })
    await sysApi.deleteUser(row.id)
    ElMessage.success('已删除用户')
    await loadUsers()
  } catch {
    // ignore cancel
  }
}

async function onReceivePurchase(po: purchaseApi.PurchaseOrder) {
  try {
    await ElMessageBox.confirm(`确认将采购单 ${po.poNo} 全部收货并入库吗？`, '提示', { type: 'warning' })
    await purchaseApi.receiveAll(po.id)
    ElMessage.success('收货完成')
    await loadPurchaseOrders()
    await loadStocks()
  } catch {
    // ignore
  }
}

async function onMarkMessageRead(row: messageApi.MessageNotice) {
  try {
    await messageApi.markRead(row.id)
    row.read = true
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

async function onScanMessages() {
  try {
    await messageApi.scanMessages()
    ElMessage.success('已触发扫描')
    await loadMessages()
  } catch (e: any) {
    ElMessage.error(e?.message || '扫描失败')
  }
}

function exportCsv(name: string, rows: string[][]) {
  const BOM = '\uFEFF'
  const csv = BOM + rows.map((r) => r.map((c) => `"${String(c).replace(/"/g, '""')}"`).join(',')).join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${name}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

function onExportReport() {
  const tabs = [
    { name: '订单', rows: [['订单号', '客户ID', '状态', '交货期', '产品物料ID', '数量', '工单号'] as string[]].concat(state.orders.map((o) => [o.orderNo, String(o.customerId), o.status ?? '', o.deliveryDate ?? '', String(o.productMaterialId), String(o.qty), o.workOrderNo ?? ''])) },
    { name: '工单', rows: [['工单号', '订单ID', '产品物料ID', '数量', '状态', '计划完成'] as string[]].concat(state.workOrders.map((w) => [w.workOrderNo, String(w.orderId), String(w.productMaterialId), String(w.qty), w.status ?? '', w.dueDate ?? ''])) },
    { name: '库存', rows: [['物料ID', '数量', '更新时间'] as string[]].concat(state.stocks.map((s) => [String(s.materialId), String(s.qty), s.updatedAt ?? ''])) },
  ]
  tabs.forEach((t) => {
    if (t.rows.length > 1) exportCsv(t.name, t.rows)
  })
  ElMessage.success('已导出 CSV')
}

function openPurchaseDialog() {
  purchaseForm.poNo = genPurchaseNo()
  purchaseForm.supplierId = undefined
  purchaseForm.expectedDate = ''
  purchaseForm.remark = ''
  purchaseForm.lines = []
  purchaseFormLine.materialId = undefined
  purchaseFormLine.qty = 0
  purchaseFormLine.price = undefined
  purchaseFormLine.remark = ''
  showPurchaseDialog.value = true
}

function addPurchaseLine() {
  if (purchaseFormLine.materialId == null || purchaseFormLine.qty <= 0) {
    ElMessage.warning('请选择物料并输入数量')
    return
  }
  purchaseForm.lines.push({
    materialId: purchaseFormLine.materialId,
    qty: purchaseFormLine.qty,
    price: purchaseFormLine.price,
    remark: purchaseFormLine.remark || undefined,
  })
  purchaseFormLine.materialId = undefined
  purchaseFormLine.qty = 0
  purchaseFormLine.price = undefined
  purchaseFormLine.remark = ''
}

function removePurchaseLine(idx: number) {
  purchaseForm.lines.splice(idx, 1)
}

async function submitPurchaseOrder() {
  if (!purchaseForm.poNo.trim()) {
    ElMessage.warning('请输入采购单号')
    return
  }
  if (purchaseForm.lines.length === 0) {
    ElMessage.warning('请至少添加一行物料')
    return
  }
  purchaseSubmitting.value = true
  try {
    await purchaseApi.createPurchaseOrder({
      poNo: purchaseForm.poNo.trim(),
      supplierId: purchaseForm.supplierId,
      expectedDate: purchaseForm.expectedDate || undefined,
      remark: purchaseForm.remark || undefined,
      lines: purchaseForm.lines.map((l) => ({ materialId: l.materialId, qty: l.qty, price: l.price, remark: l.remark })),
    })
    ElMessage.success('创建成功')
    showPurchaseDialog.value = false
    await loadPurchaseOrders()
  } catch (e: any) {
    ElMessage.error(e?.message || '创建失败')
  } finally {
    purchaseSubmitting.value = false
  }
}

onMounted(async () => {
  await Promise.all([
    loadUsers(),
    loadMaterials(),
    loadCustomers(),
    loadSuppliers(),
    loadOrders(),
    loadWorkOrders(),
    loadStocks(),
    loadReasons(),
    loadPurchaseOrders(),
    loadDevices(),
    loadMessages(),
    loadReportSummary(),
  ])
})
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
          @export-report="onExportReport"
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

.metric-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.metric-card {
  border: 0;
  border-radius: 12px;
  box-shadow: inset 0 0 0 1px #e7efff;
  background: linear-gradient(180deg, #f8fbff 0%, #eef4ff 100%);
}

.metric-card h2 {
  margin: 10px 0 0;
  color: #2f3a50;
}

.chart-wrap {
  height: 280px;
  margin-top: 12px;
  border-radius: 12px;
  padding: 8px;
  background: #fff;
  box-shadow: inset 0 0 0 1px #e7efff;
}

.tip-text {
  margin-top: 12px;
  color: #909399;
}

.soft-btn {
  border-radius: 10px;
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

