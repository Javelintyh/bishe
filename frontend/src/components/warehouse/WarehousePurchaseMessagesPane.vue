<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { Material } from '@/api/base'
import type { MessageNotice } from '@/api/message'
import * as messageApi from '@/api/message'
import * as purchaseApi from '@/api/purchase'
import type { PurchaseOrderDetail } from '@/api/purchase'
import type { ProductionWorkOrder } from '@/api/production'

export type InboundFromMessagePayload = {
  msg: MessageNotice
  poId: number
  poNo: string
  prefill: {
    materialId: number | null
    qty: number
  }
}

const props = defineProps<{
  materials: Material[]
  refreshToken: number
  pendingOutboundWorkOrders: ProductionWorkOrder[]
}>()

const emit = defineEmits<{
  (e: 'inbound-from-message', payload: InboundFromMessagePayload): void
  (
    e: 'quick-outbound-from-workorder-message',
    payload: { msg: MessageNotice; workOrderNo: string },
  ): void
  (e: 'goto-raw-material-outbound', payload: { msg: MessageNotice }): void
  (e: 'create-production-from-stock-low', payload: { msg: MessageNotice }): void
  (e: 'handle-warehouse-inbound-urge', payload: { msg: MessageNotice }): void
}>()

const messages = ref<MessageNotice[]>([])
const purchaseNoMap = ref<Record<number, string>>({})
const purchaseDetailsCache = ref<Record<number, PurchaseOrderDetail[]>>({})

const unreadCount = computed(() => messages.value.length)

function stockLowActionLabel(row: MessageNotice): string {
  if (row.noticeType !== 'STOCK_LOW') return '通知车间生产'
  const code = row.relatedId
  const material = props.materials.find((m) => m.materialCode === code)
  if (material?.materialType === 'RAW') return '去申请采购'
  return '通知车间生产'
}

const pendingWorkOrderNoSet = computed(() => {
  const s = new Set<string>()
  for (const wo of props.pendingOutboundWorkOrders ?? []) {
    if (wo.workOrderNo) s.add(wo.workOrderNo)
  }
  return s
})

async function cleanupFinishedWorkOrderMessages() {
  const active = pendingWorkOrderNoSet.value
  const toRemove = messages.value.filter(
    (m) =>
      (m.noticeType === 'WO_DUE_SOON' ||
        m.noticeType === 'WO_OVERDUE' ||
        m.noticeType === 'WO_CREATED' ||
        m.noticeType === 'WO_URGENT') &&
      m.relatedId &&
      !active.has(m.relatedId),
  )

  if (!toRemove.length) return

  for (const m of toRemove) {
    try {
      await messageApi.markRead(m.id)
    } catch {
      // ignore
    }
  }

  const removeIds = new Set(toRemove.map((m) => m.id))
  messages.value = messages.value.filter((m) => !removeIds.has(m.id))
}

const purchaseSummaryMap = computed(() => {
  const summaryMap: Record<number, string> = {}
  for (const [poIdStr, details] of Object.entries(purchaseDetailsCache.value)) {
    const poId = Number(poIdStr)
    if (!poId) continue
    if (!details?.length) continue

    const names = Array.from(
      new Set(
        details.map((d) => {
          const mat = props.materials.find((m) => m.id === d.materialId)
          return mat?.materialName ?? `ID:${d.materialId}`
        }),
      ),
    )
    summaryMap[poId] = names.join('，')
  }
  return summaryMap
})

const purchaseDetailDialogVisible = ref(false)
const purchaseDetailLoading = ref(false)
const currentPurchaseId = ref<number | null>(null)
const currentPurchaseNo = ref('')
const currentPurchaseDetails = ref<PurchaseOrderDetail[]>([])

async function loadPurchaseNoMap() {
  const poResp = await purchaseApi.listPurchaseOrders()
  const poList = poResp.data ?? []
  const noMap: Record<number, string> = {}
  for (const po of poList) {
    noMap[po.id] = po.poNo
  }
  purchaseNoMap.value = noMap
}

async function loadPurchaseDetailsCache(poIds: number[]) {
  if (!poIds.length) {
    purchaseDetailsCache.value = {}
    return
  }

  const unique = Array.from(new Set(poIds))
  const cache: Record<number, PurchaseOrderDetail[]> = {}
  await Promise.all(
    unique.map(async (poId) => {
      const detailResp = await purchaseApi.getPurchaseOrderDetails(poId)
      cache[poId] = detailResp.data ?? []
    }),
  )
  purchaseDetailsCache.value = cache
}

async function loadWarehouseMessages() {
  try {
    const resp = await messageApi.listMessages(true)
    const all = resp.data ?? []
    const msgs = all
      .filter(
        (m) =>
          m.noticeType === 'PURCHASE_ARRIVED' ||
          m.noticeType === 'STOCK_LOW' ||
          m.noticeType === 'WH_INBOUND_URGE' ||
          m.noticeType === 'CAPACITY_WARN_PRODUCT' ||
          m.noticeType === 'CAPACITY_WARN_RAW' ||
          m.noticeType === 'WO_URGENT' ||
          m.noticeType === 'WO_CREATED' ||
          m.noticeType === 'WO_DUE_SOON' ||
          m.noticeType === 'WO_OVERDUE',
      )
      .sort((a, b) => {
        const ta = new Date(a.createdAt).getTime()
        const tb = new Date(b.createdAt).getTime()
        return tb - ta
      })
      .slice(0, 50)

    messages.value = msgs

    // 仅预加载“采购到货”消息相关数据
    await loadPurchaseNoMap()
    const poIds = msgs
      .filter((m) => m.noticeType === 'PURCHASE_ARRIVED')
      .map((m) => Number(m.relatedId))
      .filter((id) => Number.isFinite(id) && id > 0)

    await loadPurchaseDetailsCache(poIds)
  } catch (e: any) {
    ElMessage.error(e?.message || '加载消息失败')
  }
}

async function openWarehousePurchaseDetail(msg: MessageNotice) {
  const poId = Number(msg.relatedId)
  if (!poId) {
    ElMessage.error('消息缺少采购单信息')
    return
  }

  currentPurchaseId.value = poId
  currentPurchaseNo.value = purchaseNoMap.value[poId] || msg.relatedId || ''
  purchaseDetailDialogVisible.value = true
  purchaseDetailLoading.value = true

  try {
    const cached = purchaseDetailsCache.value[poId]
    if (cached) {
      currentPurchaseDetails.value = cached
      return
    }

    const resp = await purchaseApi.getPurchaseOrderDetails(poId)
    const details = resp.data ?? []
    currentPurchaseDetails.value = details
    purchaseDetailsCache.value = { ...purchaseDetailsCache.value, [poId]: details }
  } catch (e: any) {
    ElMessage.error(e?.message || '获取采购明细失败')
  } finally {
    purchaseDetailLoading.value = false
  }
}

async function onWarehousePurchaseInbound(msg: MessageNotice) {
  const poId = Number(msg.relatedId)
  if (!poId) {
    ElMessage.error('消息缺少采购单信息')
    return
  }

  const cached = purchaseDetailsCache.value[poId]
  let details: PurchaseOrderDetail[] = cached ?? []

  // 没缓存时再拉一次，确保预填数据可用
  if (!details.length) {
    try {
      const resp = await purchaseApi.getPurchaseOrderDetails(poId)
      details = resp.data ?? []
      purchaseDetailsCache.value = { ...purchaseDetailsCache.value, [poId]: details }
    } catch (e: any) {
      ElMessage.error(e?.message || '获取采购明细失败')
      return
    }
  }

  const first = details[0]
  emit('inbound-from-message', {
    msg,
    poId,
    poNo: purchaseNoMap.value[poId] || msg.relatedId || '',
    prefill: {
      materialId: first?.materialId ?? null,
      qty: Number(first?.qty ?? 1) || 1,
    },
  })
}

watch(
  () => props.refreshToken,
  async () => {
    await loadWarehouseMessages()
  },
)

watch(
  () => props.pendingOutboundWorkOrders,
  async () => {
    // 工单状态变化后，如果“临期/超期消息”对应的工单已不在待出库里，则自动移除该消息
    await cleanupFinishedWorkOrderMessages()
  },
  { deep: true },
)

onMounted(async () => {
  await loadWarehouseMessages()
})
</script>

<template>
  <el-tab-pane name="messages">
    <template #label>
      <el-badge
        :value="unreadCount"
        :hidden="unreadCount === 0"
        :max="99"
        class="tab-badge"
      >
        <span>消息</span>
      </el-badge>
    </template>

    <el-table :data="messages" size="small" style="width: 100%">
      <el-table-column label="对象" width="200">
        <template #default="{ row }">
          {{
            (() => {
              if (row.noticeType === 'PURCHASE_ARRIVED') {
                const poId = Number(row.relatedId)
                if (!poId) return row.relatedId || ''
                return purchaseNoMap[poId] || row.relatedId || ''
              }
              if (row.noticeType === 'STOCK_LOW') {
                return row.relatedId || ''
              }
              if (row.noticeType === 'WH_INBOUND_URGE') {
                return row.relatedId || ''
              }
              if (row.noticeType === 'CAPACITY_WARN_PRODUCT') {
                return '成品总仓'
              }
              if (row.noticeType === 'CAPACITY_WARN_RAW') {
                return row.relatedId || ''
              }
              if (
                row.noticeType === 'WO_DUE_SOON' ||
                row.noticeType === 'WO_OVERDUE' ||
                row.noticeType === 'WO_CREATED' ||
                row.noticeType === 'WO_URGENT'
              ) {
                return row.relatedId || ''
              }
              return row.relatedId || ''
            })()
          }}
        </template>
      </el-table-column>

      <el-table-column label="提示信息" min-width="220">
        <template #default="{ row }">
          {{
            (() => {
              if (row.noticeType === 'PURCHASE_ARRIVED') {
                const poId = Number(row.relatedId)
                if (!poId) return ''
                return purchaseSummaryMap[poId] || '点击详情查看物料明细'
              }
              if (row.noticeType === 'STOCK_LOW') {
                const first = String(row.content || '').split('\n')[0]
                return first || row.title || ''
              }
              if (row.noticeType === 'WH_INBOUND_URGE') {
                const first = String(row.content || '').split('\n')[0]
                return first || row.title || ''
              }
              if (row.noticeType === 'CAPACITY_WARN_PRODUCT' || row.noticeType === 'CAPACITY_WARN_RAW') {
                const first = String(row.content || '').split('\n')[0]
                return first || row.title || ''
              }
              if (row.content) {
                // 取第一行或兜底为标题
                const first = String(row.content).split('\n')[0]
                return first || row.title || ''
              }
              return row.title || ''
            })()
          }}
        </template>
      </el-table-column>

      <el-table-column prop="createdAt" label="时间" width="170" />

      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <span v-if="row.noticeType === 'PURCHASE_ARRIVED'">
            {{ row.read ? '已入库' : '待入库' }}
          </span>
          <span v-else>
            {{
              row.noticeType === 'WO_URGENT'
                ? '加急待出库'
                : row.noticeType === 'WO_CREATED'
                  ? '待出库'
                : row.noticeType === 'STOCK_LOW'
                  ? '低库存'
                : row.noticeType === 'WH_INBOUND_URGE'
                  ? '待入库'
                : row.noticeType === 'CAPACITY_WARN_PRODUCT' || row.noticeType === 'CAPACITY_WARN_RAW'
                  ? '容量预警'
                : row.noticeType === 'WO_OVERDUE'
                  ? '超期'
                  : '临期'
            }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <template v-if="row.noticeType === 'PURCHASE_ARRIVED'">
            <el-button
              type="primary"
              link
              size="small"
              :disabled="row.read"
              @click="onWarehousePurchaseInbound(row)"
            >
              入库
            </el-button>
            <el-button
              type="primary"
              link
              size="small"
              @click="openWarehousePurchaseDetail(row)"
            >
              详情
            </el-button>
          </template>
          <template v-else-if="row.noticeType === 'STOCK_LOW'">
            <el-button
              type="primary"
              link
              size="small"
              @click="emit('create-production-from-stock-low', { msg: row })"
            >
              {{ stockLowActionLabel(row) }}
            </el-button>
          </template>
          <template v-else-if="row.noticeType === 'WH_INBOUND_URGE'">
            <el-button
              type="primary"
              link
              size="small"
              @click="emit('handle-warehouse-inbound-urge', { msg: row })"
            >
              去处理入库
            </el-button>
          </template>
          <template v-else-if="row.noticeType === 'CAPACITY_WARN_PRODUCT' || row.noticeType === 'CAPACITY_WARN_RAW'">
            <el-button
              type="primary"
              link
              size="small"
              @click="emit('goto-raw-material-outbound', { msg: row })"
            >
              {{ row.noticeType === 'CAPACITY_WARN_PRODUCT' ? '去成品出库' : '去原材料出库' }}
            </el-button>
          </template>

          <template
            v-else-if="
              row.noticeType === 'WO_DUE_SOON' ||
              row.noticeType === 'WO_OVERDUE' ||
              row.noticeType === 'WO_CREATED' ||
              row.noticeType === 'WO_URGENT'
            "
          >
            <el-button
              type="primary"
              link
              size="small"
              @click="emit('quick-outbound-from-workorder-message', { msg: row, workOrderNo: row.relatedId || '' })"
            >
              去处理工单
            </el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="purchaseDetailDialogVisible"
      :title="`采购单详情 - ${currentPurchaseNo}`"
      width="560px"
    >
      <el-table
        :data="currentPurchaseDetails"
        size="small"
        border
        :loading="purchaseDetailLoading"
        max-height="260"
      >
        <el-table-column label="物料" min-width="220">
          <template #default="{ row }">
            {{
              (() => {
                const m = props.materials.find((mm) => mm.id === row.materialId)
                if (!m) return `ID:${row.materialId}`
                return m.materialName
              })()
            }}
          </template>
        </el-table-column>
        <el-table-column prop="qty" label="采购数量" width="100" />
        <el-table-column
          prop="receivedQty"
          label="已入库数量"
          width="110"
        />
      </el-table>
      <template #footer>
        <el-button @click="purchaseDetailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </el-tab-pane>
</template>

<style scoped>
.tab-badge :deep(.el-badge__content) {
  top: 2px;
  right: -4px;
}
</style>

