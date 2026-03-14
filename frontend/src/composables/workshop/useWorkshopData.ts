/**
 * 车间端数据加载组合式函数
 */
import { computed, reactive, ref } from 'vue'
import * as productionApi from '@/api/production'
import * as baseApi from '@/api/base'
import type { WorkshopState } from '@/types/workshop'
import { DEFAULT_BAD_REASONS } from '@/constants/workshop'

/**
 * Notes:
 * - 车间端数据加载与处理逻辑
 *
 * Returns:
 * - loading: 加载状态
 * - state: 车间状态数据
 * - loadData: 加载数据函数
 * - materialMap: 物料映射
 * - productMaterials: 成品列表
 * - rawMaterials: 原材料列表
 * - reportableWorkOrders: 可报工工单
 * - getMaterialName: 获取物料名称
 * - getMaterialUnit: 获取物料单位
 * - getProductBomInfo: 获取产品BOM信息
 */
export function useWorkshopData() {
  const loading = ref(false)

  const state = reactive<WorkshopState>({
    workOrders: [],
    allWorkOrders: [],
    reports: [],
    reasons: DEFAULT_BAD_REASONS,
    materials: [],
    bomMap: {},
  })

  const materialMap = computed(() => {
    const m: Record<number, baseApi.Material> = {}
    for (const mat of state.materials) {
      m[mat.id] = mat
    }
    return m
  })

  const productMaterials = computed(() =>
    state.materials.filter((m) => m.materialType === 'PRODUCT'),
  )

  const rawMaterials = computed(() =>
    state.materials.filter((m) => m.materialType === 'RAW'),
  )

  const reportableWorkOrders = computed(() =>
    state.workOrders.filter((w) => w.status === 'PRODUCING'),
  )

  function getMaterialName(id: number): string {
    return materialMap.value[id]?.materialName ?? `ID:${id}`
  }

  function getMaterialUnit(id: number): string {
    return materialMap.value[id]?.unit ?? ''
  }

  function getProductBomInfo(productMaterialId: number): string {
    const bomLines = state.bomMap[productMaterialId] ?? []
    if (bomLines.length === 0) return '无BOM配置'
    return bomLines
      .map((line) => `${getMaterialName(line.materialId)} x${line.qty}`)
      .join(', ')
  }

  async function loadData(onLoaded?: () => void) {
    loading.value = true
    try {
      const [woRes, allWoRes, reportsRes, materialsRes] = await Promise.all([
        productionApi.listWorkOrders({ status: 'PRODUCING' }),
        productionApi.listWorkOrders(),
        productionApi.listReports(),
        baseApi.listMaterials(),
      ])
      state.workOrders = woRes.data ?? []
      state.allWorkOrders = allWoRes.data ?? []
      state.reports = reportsRes.data ?? []
      state.materials = materialsRes.data ?? []

      const bomMapTemp: Record<number, baseApi.BomLine[]> = {}
      for (const prod of productMaterials.value) {
        const bomRes = await baseApi.listBom(prod.id)
        bomMapTemp[prod.id] = bomRes.data ?? []
      }
      state.bomMap = bomMapTemp

      onLoaded?.()
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    state,
    loadData,
    materialMap,
    productMaterials,
    rawMaterials,
    reportableWorkOrders,
    getMaterialName,
    getMaterialUnit,
    getProductBomInfo,
  }
}
