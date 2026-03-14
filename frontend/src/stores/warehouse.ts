/**
 * 仓库端状态管理
 */
import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'
import * as inventoryApi from '@/api/inventory'
import * as baseApi from '@/api/base'
import * as productionApi from '@/api/production'
import * as orderApi from '@/api/order'
import type { WarehouseState, BomItem } from '@/types/warehouse'
import { getTodayStr } from '@/utils/format'

export const useWarehouseStore = defineStore('warehouse', () => {
  const loading = ref(false)

  const state = reactive<WarehouseState>({
    stocks: [],
    materials: [],
    records: [],
    workOrders: [],
    allWorkOrders: [],
    orders: [],
    bomMap: {},
  })

  const stockMap = computed(() => {
    const m: Record<number, number> = {}
    for (const s of state.stocks) {
      m[s.materialId] = s.qty
    }
    return m
  })

  const materialMap = computed(() => {
    const m: Record<number, baseApi.Material> = {}
    for (const mat of state.materials) {
      m[mat.id] = mat
    }
    return m
  })

  const rawMaterials = computed(() => state.materials.filter((m) => m.materialType === 'RAW'))

  const productMaterials = computed(() => state.materials.filter((m) => m.materialType === 'PRODUCT'))

  const pendingOutboundWorkOrders = computed(() =>
    state.workOrders.filter((w) => w.status === 'TO_PRODUCE'),
  )

  const pendingInboundWorkOrders = computed(() =>
    state.allWorkOrders.filter((w) => w.status === 'DONE'),
  )

  const pendingShipOrders = computed(() =>
    state.orders.filter((o) => o.status === 'COMPLETED'),
  )

  const todayRecords = computed(() => {
    const today = getTodayStr()
    return state.records.filter((r) => r.createdAt?.startsWith(today))
  })

  function getStockQty(materialId: number): number {
    return stockMap.value[materialId] ?? 0
  }

  function getMaterialInfo(materialId: number): string {
    const m = materialMap.value[materialId]
    if (!m) return `ID:${materialId}`
    return `${m.materialName}${m.materialSpec ? ` (${m.materialSpec})` : ''}`
  }

  function getMaterialName(materialId: number): string {
    return materialMap.value[materialId]?.materialName ?? `ID:${materialId}`
  }

  function getWorkOrderBomItems(wo: productionApi.ProductionWorkOrder): BomItem[] {
    const bomLines = state.bomMap[wo.productMaterialId] ?? []
    if (bomLines.length === 0) return []

    const productStock = getStockQty(wo.productMaterialId)
    const needProduceQty = Math.max(0, wo.qty - productStock)

    return bomLines.map((line) => {
      const requiredQty = line.qty * needProduceQty
      const stockQty = getStockQty(line.materialId)
      return {
        materialId: line.materialId,
        requiredQty,
        stockQty,
        shortageQty: Math.max(0, requiredQty - stockQty),
      }
    })
  }

  function isWorkOrderStockSufficient(wo: productionApi.ProductionWorkOrder): boolean {
    const productStock = getStockQty(wo.productMaterialId)
    if (productStock >= wo.qty) return true

    const bomItems = getWorkOrderBomItems(wo)
    if (bomItems.length === 0) return true
    return bomItems.every((item) => item.shortageQty === 0)
  }

  function getWorkOrderStockStatus(wo: productionApi.ProductionWorkOrder): string {
    const productStock = getStockQty(wo.productMaterialId)
    if (productStock >= wo.qty) return `成品充足 (库存:${productStock})`

    const bomItems = getWorkOrderBomItems(wo)
    if (bomItems.length === 0) return '无BOM配置'

    const shortages = bomItems.filter((item) => item.shortageQty > 0)
    if (shortages.length === 0) {
      if (productStock > 0) {
        return `成品${productStock}个+原材料充足`
      }
      return '原材料充足'
    }
    return `缺料: ${shortages.map((s) => `${getMaterialName(s.materialId)}缺${s.shortageQty}`).join(', ')}`
  }

  async function loadData() {
    loading.value = true
    try {
      const [stocksRes, materialsRes, recordsRes, workOrdersRes, allWorkOrdersRes, ordersRes] = await Promise.all([
        inventoryApi.listStocks(),
        baseApi.listMaterials(),
        inventoryApi.listInventoryRecords(),
        productionApi.listWorkOrders({ status: '' }),
        productionApi.listWorkOrders(),
        orderApi.listOrders(),
      ])
      state.stocks = stocksRes.data ?? []
      state.materials = materialsRes.data ?? []
      state.records = recordsRes.data ?? []
      state.workOrders = workOrdersRes.data ?? []
      state.allWorkOrders = allWorkOrdersRes.data ?? []
      state.orders = ordersRes.data ?? []

      const bomMapTemp: Record<number, baseApi.BomLine[]> = {}
      for (const prod of productMaterials.value) {
        const bomRes = await baseApi.listBom(prod.id)
        bomMapTemp[prod.id] = bomRes.data ?? []
      }
      state.bomMap = bomMapTemp
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    state,
    stockMap,
    materialMap,
    rawMaterials,
    productMaterials,
    pendingOutboundWorkOrders,
    pendingInboundWorkOrders,
    pendingShipOrders,
    todayRecords,
    loadData,
    getStockQty,
    getMaterialInfo,
    getMaterialName,
    getWorkOrderBomItems,
    isWorkOrderStockSufficient,
    getWorkOrderStockStatus,
  }
})
