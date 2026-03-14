<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as inventoryApi from '@/api/inventory'
import * as baseApi from '@/api/base'
import * as productionApi from '@/api/production'
import * as orderApi from '@/api/order'
import { bizTypeLabel, orderStatusLabel, workOrderStatusLabel } from '@/views/admin/helpers'

const PAGE_SIZE = 5

const activeTab = ref('stocks')

const loading = ref(false)
const state = reactive({
  stocks: [] as inventoryApi.InventoryStock[],
  materials: [] as baseApi.Material[],
  records: [] as inventoryApi.InventoryRecord[],
  workOrders: [] as productionApi.ProductionWorkOrder[],
  allWorkOrders: [] as productionApi.ProductionWorkOrder[],
  orders: [] as orderApi.OrderVO[],
  bomMap: {} as Record<number, baseApi.BomLine[]>,
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

onMounted(loadData)

function todayStr(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const todayRecords = computed(() => {
  const today = todayStr()
  return state.records.filter((r) => r.createdAt?.startsWith(today))
})

const stocksPage = ref(1)
const pagedStocks = computed(() => {
  const start = (stocksPage.value - 1) * PAGE_SIZE
  return state.stocks.slice(start, start + PAGE_SIZE)
})

const recordsPage = ref(1)
const pagedRecords = computed(() => {
  const start = (recordsPage.value - 1) * PAGE_SIZE
  return todayRecords.value.slice(start, start + PAGE_SIZE)
})

const workOrdersPage = ref(1)
const pagedWorkOrders = computed(() => {
  const start = (workOrdersPage.value - 1) * PAGE_SIZE
  return pendingOutboundWorkOrders.value.slice(start, start + PAGE_SIZE)
})

const pendingInboundPage = ref(1)
const pagedPendingInbound = computed(() => {
  const start = (pendingInboundPage.value - 1) * PAGE_SIZE
  return pendingInboundWorkOrders.value.slice(start, start + PAGE_SIZE)
})

const pendingShipPage = ref(1)
const pagedPendingShip = computed(() => {
  const start = (pendingShipPage.value - 1) * PAGE_SIZE
  return pendingShipOrders.value.slice(start, start + PAGE_SIZE)
})

interface BomItem {
  materialId: number
  requiredQty: number
  stockQty: number
  shortageQty: number
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

async function onQuickOutbound(wo: productionApi.ProductionWorkOrder) {
  const productStock = getStockQty(wo.productMaterialId)

  if (productStock >= wo.qty) {
    try {
      await inventoryApi.outbound({
        materialId: wo.productMaterialId,
        qty: wo.qty,
        bizType: 'SALES_OUT',
        bizId: wo.workOrderNo,
      })
      await productionApi.updateWorkOrderForProduce(wo.id, 'STORED', 0)

      const relatedOrder = state.orders.find((o) => o.id === wo.orderId)
      if (relatedOrder) {
        await orderApi.updateOrderStatus(relatedOrder.id, 'SHIPPED')
      }

      ElMessage.success('成品出库成功，订单已发货')
      await loadData()
    } catch (e: any) {
      ElMessage.error(e.response?.data?.message || '出库失败')
    }
    return
  }

  const bomItems = getWorkOrderBomItems(wo)
  if (bomItems.length === 0) {
    ElMessage.warning('该产品无BOM配置，无法出库原材料')
    return
  }

  const shortages = bomItems.filter((item) => item.shortageQty > 0)
  if (shortages.length > 0) {
    ElMessage.error(`原材料不足，无法出库: ${shortages.map((s) => `${getMaterialName(s.materialId)}缺${s.shortageQty}`).join(', ')}`)
    return
  }

  try {
    if (productStock > 0) {
      await inventoryApi.outbound({
        materialId: wo.productMaterialId,
        qty: productStock,
        bizType: 'SALES_OUT',
        bizId: wo.workOrderNo,
      })
    }

    for (const item of bomItems) {
      if (item.requiredQty > 0) {
        await inventoryApi.outbound({
          materialId: item.materialId,
          qty: item.requiredQty,
          bizType: 'PRODUCTION_OUT',
          bizId: wo.workOrderNo,
        })
      }
    }

    const needProduceQty = wo.qty - productStock
    await productionApi.updateWorkOrderForProduce(wo.id, 'PRODUCING', needProduceQty)

    const relatedOrder = state.orders.find((o) => o.id === wo.orderId)
    if (relatedOrder) {
      await orderApi.updateOrderStatus(relatedOrder.id, 'PRODUCING')
    }

    if (productStock > 0) {
      ElMessage.success(`已出库成品${productStock}个，原材料已发往车间生产${needProduceQty}个`)
    } else {
      ElMessage.success('原材料出库成功，已发往车间生产')
    }
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '出库失败')
  }
}

async function onQuickInbound(wo: productionApi.ProductionWorkOrder) {
  try {
    await inventoryApi.inbound({
      materialId: wo.productMaterialId,
      qty: wo.qty,
      bizType: 'PRODUCTION_IN',
      bizId: wo.workOrderNo,
    })
    await productionApi.updateWorkOrderStatus(wo.id, 'STORED')

    const relatedOrder = state.orders.find((o) => o.id === wo.orderId)
    if (relatedOrder) {
      await orderApi.updateOrderStatus(relatedOrder.id, 'COMPLETED')
    }

    ElMessage.success('成品入库成功')
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '入库失败')
  }
}

async function onShipOrder(order: orderApi.OrderVO) {
  const relatedWorkOrder = state.allWorkOrders.find((w) => w.orderId === order.id)
  if (!relatedWorkOrder) {
    ElMessage.error('未找到关联工单')
    return
  }

  try {
    const shipQty = relatedWorkOrder.qty
    if (shipQty > 0) {
      await inventoryApi.outbound({
        materialId: relatedWorkOrder.productMaterialId,
        qty: shipQty,
        bizType: 'SALES_OUT',
        bizId: order.orderNo,
      })
    }
    await orderApi.updateOrderStatus(order.id, 'SHIPPED')
    ElMessage.success('发货成功')
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '发货失败')
  }
}

const inboundForm = reactive({
  workOrderNo: '',
  materialId: null as number | null,
  qty: 1,
})

function onSelectInboundWorkOrder(workOrderNo: string) {
  const wo = pendingInboundWorkOrders.value.find((w) => w.workOrderNo === workOrderNo)
  if (wo) {
    inboundForm.materialId = wo.productMaterialId
    inboundForm.qty = wo.qty
  }
}

async function onInbound() {
  if (!inboundForm.materialId || inboundForm.qty <= 0) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    await inventoryApi.inbound({
      materialId: inboundForm.materialId,
      qty: inboundForm.qty,
      bizType: 'PRODUCTION_IN',
      bizId: inboundForm.workOrderNo || undefined,
    })
    ElMessage.success('入库成功')
    inboundForm.workOrderNo = ''
    inboundForm.materialId = null
    inboundForm.qty = 1
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '入库失败')
  }
}

const salesOutboundForm = reactive({
  materialId: null as number | null,
  qty: 1,
  bizId: '',
})

async function onSalesOutbound() {
  if (!salesOutboundForm.materialId || salesOutboundForm.qty <= 0) {
    ElMessage.warning('请填写完整信息')
    return
  }
  const stock = getStockQty(salesOutboundForm.materialId)
  if (salesOutboundForm.qty > stock) {
    ElMessage.error(`库存不足，当前库存: ${stock}`)
    return
  }
  try {
    await inventoryApi.outbound({
      materialId: salesOutboundForm.materialId,
      qty: salesOutboundForm.qty,
      bizType: 'SALES_OUT',
      bizId: salesOutboundForm.bizId || undefined,
    })
    ElMessage.success('出库成功')
    salesOutboundForm.materialId = null
    salesOutboundForm.qty = 1
    salesOutboundForm.bizId = ''
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '出库失败')
  }
}

const purchaseInboundForm = reactive({
  materialId: null as number | null,
  qty: 1,
  bizId: '',
})

async function onPurchaseInbound() {
  if (!purchaseInboundForm.materialId || purchaseInboundForm.qty <= 0) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    await inventoryApi.inbound({
      materialId: purchaseInboundForm.materialId,
      qty: purchaseInboundForm.qty,
      bizType: 'PURCHASE_IN',
      bizId: purchaseInboundForm.bizId || undefined,
    })
    ElMessage.success('入库成功')
    purchaseInboundForm.materialId = null
    purchaseInboundForm.qty = 1
    purchaseInboundForm.bizId = ''
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '入库失败')
  }
}

const outboundForm = reactive({
  workOrderNo: '',
  materialId: null as number | null,
  qty: 1,
})

function onSelectOutboundWorkOrder(workOrderNo: string) {
  const wo = pendingOutboundWorkOrders.value.find((w) => w.workOrderNo === workOrderNo)
  if (wo) {
    outboundForm.materialId = wo.productMaterialId
    outboundForm.qty = wo.qty
  }
}

async function onOutbound() {
  if (!outboundForm.materialId || outboundForm.qty <= 0) {
    ElMessage.warning('请填写完整信息')
    return
  }
  const stock = getStockQty(outboundForm.materialId)
  if (outboundForm.qty > stock) {
    ElMessage.error(`库存不足，当前库存: ${stock}`)
    return
  }
  try {
    await inventoryApi.outbound({
      materialId: outboundForm.materialId,
      qty: outboundForm.qty,
      bizType: 'PRODUCTION_OUT',
      bizId: outboundForm.workOrderNo || undefined,
    })
    ElMessage.success('出库成功')
    outboundForm.workOrderNo = ''
    outboundForm.materialId = null
    outboundForm.qty = 1
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '出库失败')
  }
}

const adjustForm = reactive({
  materialId: null as number | null,
  adjustQty: 0,
  remark: '',
})

async function onAdjust() {
  if (!adjustForm.materialId) {
    ElMessage.warning('请选择物料')
    return
  }
  if (adjustForm.adjustQty === 0) {
    ElMessage.warning('调整数量不能为0')
    return
  }
  try {
    if (adjustForm.adjustQty > 0) {
      await inventoryApi.inbound({
        materialId: adjustForm.materialId,
        qty: adjustForm.adjustQty,
        bizType: 'ADJUST',
      })
    } else {
      await inventoryApi.outbound({
        materialId: adjustForm.materialId,
        qty: Math.abs(adjustForm.adjustQty),
        bizType: 'ADJUST',
      })
    }
    ElMessage.success('盘点调整成功')
    adjustForm.materialId = null
    adjustForm.adjustQty = 0
    adjustForm.remark = ''
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '调整失败')
  }
}
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
          <el-button class="soft-btn" type="primary" :loading="loading" @click="loadData">刷新数据</el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab" tab-position="left" class="page-tabs" style="min-height: 420px">
        <!-- 库存总览 -->
        <el-tab-pane label="库存总览" name="stocks">
          <el-table :data="pagedStocks" v-loading="loading" border stripe>
            <el-table-column label="物料编码" prop="materialId">
              <template #default="{ row }">
                {{ materialMap[row.materialId]?.materialCode ?? row.materialId }}
              </template>
            </el-table-column>
            <el-table-column label="物料名称">
              <template #default="{ row }">
                {{ getMaterialInfo(row.materialId) }}
              </template>
            </el-table-column>
            <el-table-column label="类型">
              <template #default="{ row }">
                {{ materialMap[row.materialId]?.materialType === 'RAW' ? '原材料' : '成品' }}
              </template>
            </el-table-column>
            <el-table-column label="库存数量" prop="qty" />
            <el-table-column label="单位">
              <template #default="{ row }">
                {{ materialMap[row.materialId]?.unit ?? '-' }}
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="state.stocks.length > PAGE_SIZE"
            v-model:current-page="stocksPage"
            :page-size="PAGE_SIZE"
            :total="state.stocks.length"
            layout="prev, pager, next"
            style="margin-top: 12px; justify-content: flex-end"
          />
        </el-tab-pane>

        <!-- 今日出入库记录 -->
        <el-tab-pane label="今日出入库记录" name="todayRecords">
          <el-table :data="pagedRecords" v-loading="loading" border stripe>
            <el-table-column label="物料">
              <template #default="{ row }">
                {{ getMaterialInfo(row.materialId) }}
              </template>
            </el-table-column>
            <el-table-column label="变动数量" prop="changeQty" />
            <el-table-column label="业务类型">
              <template #default="{ row }">
                {{ bizTypeLabel(row.bizType) }}
              </template>
            </el-table-column>
            <el-table-column label="业务单号" prop="bizId" />
            <el-table-column label="时间" prop="createdAt" />
          </el-table>
          <el-pagination
            v-if="todayRecords.length > PAGE_SIZE"
            v-model:current-page="recordsPage"
            :page-size="PAGE_SIZE"
            :total="todayRecords.length"
            layout="prev, pager, next"
            style="margin-top: 12px; justify-content: flex-end"
          />
        </el-tab-pane>

        <!-- 工单待出库 -->
        <el-tab-pane name="workOrders">
          <template #label>
            <el-badge :value="pendingOutboundWorkOrders.length" :hidden="pendingOutboundWorkOrders.length === 0" :max="99" class="tab-badge">
              <span>工单待出库</span>
            </el-badge>
          </template>
          <el-table :data="pagedWorkOrders" v-loading="loading" border stripe>
            <el-table-column label="工单号" prop="workOrderNo" />
            <el-table-column label="产品">
              <template #default="{ row }">
                {{ getMaterialInfo(row.productMaterialId) }}
              </template>
            </el-table-column>
            <el-table-column label="需求数量" prop="qty" />
            <el-table-column label="状态">
              <template #default="{ row }">
                {{ workOrderStatusLabel(row.status) }}
              </template>
            </el-table-column>
            <el-table-column label="库存状态" min-width="200">
              <template #default="{ row }">
                {{ getWorkOrderStockStatus(row) }}
              </template>
            </el-table-column>
            <el-table-column label="所需原材料" min-width="250">
              <template #default="{ row }">
                <div v-for="item in getWorkOrderBomItems(row)" :key="item.materialId" style="font-size: 12px;">
                  {{ getMaterialName(item.materialId) }}: 需{{ item.requiredQty }} / 库存{{ item.stockQty }}
                  <span v-if="item.shortageQty > 0" style="color: red;">(缺{{ item.shortageQty }})</span>
                </div>
                <span v-if="getWorkOrderBomItems(row).length === 0" style="color: #999;">无BOM配置</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  size="small"
                  :disabled="!isWorkOrderStockSufficient(row)"
                  @click="onQuickOutbound(row)"
                >
                  一键出库
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="pendingOutboundWorkOrders.length > PAGE_SIZE"
            v-model:current-page="workOrdersPage"
            :page-size="PAGE_SIZE"
            :total="pendingOutboundWorkOrders.length"
            layout="prev, pager, next"
            style="margin-top: 12px; justify-content: flex-end"
          />
        </el-tab-pane>

        <!-- 成品待入库 -->
        <el-tab-pane name="pendingInbound">
          <template #label>
            <el-badge :value="pendingInboundWorkOrders.length" :hidden="pendingInboundWorkOrders.length === 0" :max="99" class="tab-badge">
              <span>成品待入库</span>
            </el-badge>
          </template>
          <el-table :data="pagedPendingInbound" v-loading="loading" border stripe>
            <el-table-column label="工单号" prop="workOrderNo" />
            <el-table-column label="产品">
              <template #default="{ row }">
                {{ getMaterialInfo(row.productMaterialId) }}
              </template>
            </el-table-column>
            <el-table-column label="生产数量" prop="qty" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button type="success" size="small" @click="onQuickInbound(row)">
                  一键入库
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="pendingInboundWorkOrders.length > PAGE_SIZE"
            v-model:current-page="pendingInboundPage"
            :page-size="PAGE_SIZE"
            :total="pendingInboundWorkOrders.length"
            layout="prev, pager, next"
            style="margin-top: 12px; justify-content: flex-end"
          />
        </el-tab-pane>

        <!-- 待发货 -->
        <el-tab-pane name="pendingShip">
          <template #label>
            <el-badge :value="pendingShipOrders.length" :hidden="pendingShipOrders.length === 0" :max="99" class="tab-badge">
              <span>待发货</span>
            </el-badge>
          </template>
          <el-table :data="pagedPendingShip" v-loading="loading" border stripe>
            <el-table-column label="订单号" prop="orderNo" />
            <el-table-column label="产品">
              <template #default="{ row }">
                {{ getMaterialInfo(row.productMaterialId) }}
              </template>
            </el-table-column>
            <el-table-column label="数量" prop="qty" />
            <el-table-column label="状态">
              <template #default="{ row }">
                <el-tag type="success">{{ orderStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button type="warning" size="small" @click="onShipOrder(row)">
                  发货
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="pendingShipOrders.length > PAGE_SIZE"
            v-model:current-page="pendingShipPage"
            :page-size="PAGE_SIZE"
            :total="pendingShipOrders.length"
            layout="prev, pager, next"
            style="margin-top: 12px; justify-content: flex-end"
          />
        </el-tab-pane>

        <!-- 成品管理 -->
        <el-tab-pane label="成品管理" name="product">
          <el-row :gutter="24">
            <el-col :span="12">
              <h4 style="color: #67c23a; margin-bottom: 16px;">成品入库</h4>
              <el-form :model="inboundForm" label-width="100px">
                <el-form-item label="关联工单">
                  <el-select
                    v-model="inboundForm.workOrderNo"
                    placeholder="可选择工单"
                    clearable
                    filterable
                    @change="onSelectInboundWorkOrder"
                  >
                    <el-option
                      v-for="wo in pendingInboundWorkOrders"
                      :key="wo.workOrderNo"
                      :label="`${wo.workOrderNo} - ${getMaterialInfo(wo.productMaterialId)}`"
                      :value="wo.workOrderNo"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="成品">
                  <el-select v-model="inboundForm.materialId" placeholder="选择成品" filterable>
                    <el-option
                      v-for="m in productMaterials"
                      :key="m.id"
                      :label="`${m.materialName}${m.materialSpec ? ` (${m.materialSpec})` : ''}`"
                      :value="m.id"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="入库数量">
                  <el-input-number v-model="inboundForm.qty" :min="1" />
                </el-form-item>
                <el-form-item>
                  <el-button type="success" @click="onInbound">确认入库</el-button>
                </el-form-item>
              </el-form>
            </el-col>
            <el-col :span="12">
              <h4 style="color: #f56c6c; margin-bottom: 16px;">成品出库</h4>
              <el-form :model="salesOutboundForm" label-width="100px">
                <el-form-item label="成品">
                  <el-select v-model="salesOutboundForm.materialId" placeholder="选择成品" filterable>
                    <el-option
                      v-for="m in productMaterials"
                      :key="m.id"
                      :label="`${m.materialName}${m.materialSpec ? ` (${m.materialSpec})` : ''} [库存: ${getStockQty(m.id)}]`"
                      :value="m.id"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="出库数量">
                  <el-input-number v-model="salesOutboundForm.qty" :min="1" />
                </el-form-item>
                <el-form-item label="销售单号">
                  <el-input v-model="salesOutboundForm.bizId" placeholder="可选" />
                </el-form-item>
                <el-form-item>
                  <el-button type="danger" @click="onSalesOutbound">确认出库</el-button>
                </el-form-item>
              </el-form>
            </el-col>
          </el-row>
        </el-tab-pane>

        <!-- 原材料管理 -->
        <el-tab-pane label="原材料管理" name="rawMaterial">
          <el-row :gutter="24">
            <el-col :span="12">
              <h4 style="color: #67c23a; margin-bottom: 16px;">原材料入库（采购到货）</h4>
              <el-form :model="purchaseInboundForm" label-width="100px">
                <el-form-item label="原材料">
                  <el-select v-model="purchaseInboundForm.materialId" placeholder="选择原材料" filterable>
                    <el-option
                      v-for="m in rawMaterials"
                      :key="m.id"
                      :label="`${m.materialName}${m.materialSpec ? ` (${m.materialSpec})` : ''}`"
                      :value="m.id"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="入库数量">
                  <el-input-number v-model="purchaseInboundForm.qty" :min="1" />
                </el-form-item>
                <el-form-item label="采购单号">
                  <el-input v-model="purchaseInboundForm.bizId" placeholder="可选" />
                </el-form-item>
                <el-form-item>
                  <el-button type="success" @click="onPurchaseInbound">确认入库</el-button>
                </el-form-item>
              </el-form>
            </el-col>
            <el-col :span="12">
              <h4 style="color: #f56c6c; margin-bottom: 16px;">原材料出库（生产领料）</h4>
              <el-form :model="outboundForm" label-width="100px">
                <el-form-item label="关联工单">
                  <el-select
                    v-model="outboundForm.workOrderNo"
                    placeholder="可选择工单"
                    clearable
                    filterable
                    @change="onSelectOutboundWorkOrder"
                  >
                    <el-option
                      v-for="wo in pendingOutboundWorkOrders"
                      :key="wo.workOrderNo"
                      :label="`${wo.workOrderNo} - ${getMaterialInfo(wo.productMaterialId)}`"
                      :value="wo.workOrderNo"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="原材料">
                  <el-select v-model="outboundForm.materialId" placeholder="选择原材料" filterable>
                    <el-option
                      v-for="m in rawMaterials"
                      :key="m.id"
                      :label="`${m.materialName}${m.materialSpec ? ` (${m.materialSpec})` : ''} [库存: ${getStockQty(m.id)}]`"
                      :value="m.id"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="出库数量">
                  <el-input-number v-model="outboundForm.qty" :min="1" />
                </el-form-item>
                <el-form-item>
                  <el-button type="danger" @click="onOutbound">确认出库</el-button>
                </el-form-item>
              </el-form>
            </el-col>
          </el-row>
        </el-tab-pane>

        <!-- 盘点调整 -->
        <el-tab-pane label="盘点调整" name="adjust">
          <el-form :model="adjustForm" label-width="100px" style="max-width: 400px;">
            <el-form-item label="物料">
              <el-select v-model="adjustForm.materialId" placeholder="选择物料" filterable>
                <el-option
                  v-for="m in state.materials"
                  :key="m.id"
                  :label="`${m.materialName}${m.materialSpec ? ` (${m.materialSpec})` : ''} [库存: ${getStockQty(m.id)}]`"
                  :value="m.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="调整数量">
              <el-input-number v-model="adjustForm.adjustQty" />
              <span style="margin-left: 8px; color: #909399;">正数入库，负数出库</span>
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="adjustForm.remark" type="textarea" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="onAdjust">确认调整</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
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
