/**
 * 管理端数据加载组合式函数
 */
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import * as sysApi from '@/api/sys'
import * as baseApi from '@/api/base'
import * as orderApi from '@/api/order'
import * as productionApi from '@/api/production'
import * as inventoryApi from '@/api/inventory'
import * as qualityApi from '@/api/quality'
import * as purchaseApi from '@/api/purchase'
import * as deviceApi from '@/api/device'
import * as messageApi from '@/api/message'
import * as reportApi from '@/api/report'
import type { AdminState } from '@/types/admin'

/**
 * Notes:
 * - 管理端数据加载与状态管理
 *
 * Returns:
 * - state: 管理端状态数据
 * - loadUsers/loadMaterials/...: 各模块数据加载函数
 * - refreshCurrentTab: 刷新当前Tab数据
 */
export function useAdminData() {
  const state = reactive<AdminState>({
    users: [],
    materials: [],
    customers: [],
    suppliers: [],
    orders: [],
    workOrders: [],
    stocks: [],
    reasons: [],
    purchaseOrders: [],
    devices: [],
    messages: [],
    orderSummary: null,
    productionSummary: null,
    inventoryTurnover: null,
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

  async function onOrderRefresh() {
    await Promise.all([loadOrders(), loadWorkOrders()])
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

  async function loadAllData() {
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
  }

  return {
    state,
    loadUsers,
    loadMaterials,
    loadCustomers,
    loadSuppliers,
    loadOrders,
    loadWorkOrders,
    loadStocks,
    loadReasons,
    loadPurchaseOrders,
    loadDevices,
    loadMessages,
    loadReportSummary,
    onOrderRefresh,
    refreshCurrentTab,
    loadAllData,
  }
}
