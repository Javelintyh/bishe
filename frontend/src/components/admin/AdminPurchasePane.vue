<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as baseApi from '@/api/base'
import * as purchaseApi from '@/api/purchase'
import type { Material, Supplier } from '@/api/base'
import type { PurchaseOrderDetail, PurchaseOrder } from '@/api/purchase'
import type { PurchaseForm, PurchaseFormLineInput } from '@/types/admin'
import { purchaseStatusLabel } from '@/constants/admin'

const props = defineProps<{
  purchaseOrders: PurchaseOrder[]
  suppliers: Supplier[]
  materials: Material[]

  onMarkPurchased: (row: PurchaseOrder) => void

  showPurchaseDialog: boolean
  purchaseForm: PurchaseForm
  purchaseFormLine: PurchaseFormLineInput
  purchaseSubmitting: boolean
  openPurchaseDialog: () => void
  addPurchaseLine: () => void
  removePurchaseLine: (idx: number) => void
  submitPurchaseOrder: () => Promise<boolean>
  closePurchaseDialog: () => void
}>()

// 给模板保留原先的“扁平变量名”，避免改大段模板代码
const purchaseOrders = computed(() => props.purchaseOrders)
const suppliers = computed(() => props.suppliers)
const materials = computed(() => props.materials)
const showPurchaseDialog = computed(() => props.showPurchaseDialog)
const purchaseSubmitting = computed(() => props.purchaseSubmitting)

const purchaseForm = props.purchaseForm
const purchaseFormLine = props.purchaseFormLine
const openPurchaseDialog = props.openPurchaseDialog
const addPurchaseLine = props.addPurchaseLine
const removePurchaseLine = props.removePurchaseLine
const submitPurchaseOrder = props.submitPurchaseOrder
const closePurchaseDialog = props.closePurchaseDialog
const onMarkPurchased = props.onMarkPurchased

const showPurchaseDetailDialog = ref(false)
const purchaseDetailLoading = ref(false)
const currentPurchaseOrder = ref<PurchaseOrder | null>(null)
const currentPurchaseDetails = ref<PurchaseOrderDetail[]>([])

async function openPurchaseDetailDialog(row: PurchaseOrder) {
  currentPurchaseOrder.value = row
  showPurchaseDetailDialog.value = true
  purchaseDetailLoading.value = true
  try {
    const resp = await purchaseApi.getPurchaseOrderDetails(row.id)
    currentPurchaseDetails.value = resp.data ?? []
  } catch (e: any) {
    ElMessage.error(e?.message || '获取采购明细失败')
  } finally {
    purchaseDetailLoading.value = false
  }
}

function closePurchaseDetailDialog() {
  showPurchaseDetailDialog.value = false
}

type RawMaterialOption = {
  id: number
  materialCode: string
  materialName: string
  materialSpec?: string | null
  materialType: string
}

const rawMaterialsForSupplier = ref<RawMaterialOption[]>([])

const allRawMaterials = computed<RawMaterialOption[]>(() =>
  materials.value
    .filter((m) => m.materialType === 'RAW')
    .map((m) => ({
      id: m.id,
      materialCode: m.materialCode,
      materialName: m.materialName,
      materialSpec: m.materialSpec ?? null,
      materialType: m.materialType,
    })),
)

const supplierFilterToken = ref(0)
watch(
  [() => purchaseForm.supplierId, allRawMaterials],
  async ([supplierId, rawMaterials]) => {
    const token = ++supplierFilterToken.value

    if (supplierId == null) {
      // 未选择公司时：允许选择任意原材料（由“反向联动”去限制供应商）
      rawMaterialsForSupplier.value = rawMaterials
      return
    }

    const resp = await baseApi.listRawMaterialsBySupplier(supplierId)
    if (token !== supplierFilterToken.value) return

    if (resp.code !== 0) {
      ElMessage.error(resp.message || '加载可售原材料失败')
      return
    }

    rawMaterialsForSupplier.value = (resp.data ?? []) as RawMaterialOption[]
    if (!rawMaterialsForSupplier.value.length) {
      ElMessage.warning('该供应商未配置可售原材料，请到“供应商”页面先配置')
    }
    const allowedIds = new Set(rawMaterialsForSupplier.value.map((m) => m.id))

    const beforeLen = purchaseForm.lines.length
    purchaseForm.lines = purchaseForm.lines.filter((l) => allowedIds.has(l.materialId))
    if (purchaseForm.lines.length !== beforeLen) {
      // 公司切换后，自动清理不属于其可售范围的明细行
      ElMessage.warning('已自动清理不属于该公司可售范围的明细行')
    }

    if (purchaseFormLine.materialId != null && !allowedIds.has(purchaseFormLine.materialId)) {
      purchaseFormLine.materialId = undefined
      purchaseFormLine.qty = 0
      purchaseFormLine.price = undefined
      purchaseFormLine.remark = ''
    }
  },
  { immediate: true },
)

// 反向联动：当用户选择了物料后，限制“供应商/公司”下拉只显示能售卖该物料的公司
const filteredSuppliers = ref<Supplier[]>([])
const supplierIdsCacheByMaterial = new Map<number, number[]>() // materialId -> supplierIds

async function getSupplierIdsByMaterial(materialId: number): Promise<number[]> {
  const cached = supplierIdsCacheByMaterial.get(materialId)
  if (cached) return cached

  const resp = await baseApi.listSuppliersByRawMaterial(materialId)
  if (resp.code !== 0) {
    supplierIdsCacheByMaterial.set(materialId, [])
    return []
  }

  const ids = (resp.data ?? []).map((s) => s.id)
  supplierIdsCacheByMaterial.set(materialId, ids)
  return ids
}

async function intersectSupplierIds(materialIds: number[]): Promise<Set<number> | null> {
  const unique = Array.from(new Set(materialIds))
  if (!unique.length) return null

  let current: Set<number> | null = null
  for (const materialId of unique) {
    const ids = await getSupplierIdsByMaterial(materialId)
    const next = new Set(ids)
    if (current == null) {
      current = next
    } else {
      const currentSet = current as Set<number>
      current = new Set(Array.from(currentSet).filter((id) => next.has(id)))
    }
    if (current.size === 0) break
  }
  return current
}

const supplierIntersectionToken = ref(0)
watch(
  () => ({
    lineMaterials: purchaseForm.lines.map((l) => l.materialId),
    draftMaterial: purchaseFormLine.materialId,
  }),
  async ({ lineMaterials, draftMaterial }) => {
    const token = ++supplierIntersectionToken.value

    const materialIds = [
      ...lineMaterials,
      draftMaterial != null ? draftMaterial : undefined,
    ].filter((x): x is number => typeof x === 'number')

    const allowedSupplierIds = await intersectSupplierIds(materialIds)
    if (token !== supplierIntersectionToken.value) return

    if (allowedSupplierIds == null) {
      filteredSuppliers.value = suppliers.value
      return
    }

    filteredSuppliers.value = suppliers.value.filter((s) => allowedSupplierIds.has(s.id))

    if (purchaseForm.supplierId != null && !allowedSupplierIds.has(purchaseForm.supplierId)) {
      // 供应商不再满足已选物料：清空供应商，等用户重新选择
      purchaseForm.supplierId = undefined
    }
  },
  { immediate: true, deep: true },
)

</script>

<template>
  <div>
    <div class="action-bar">
      <el-button size="small" type="primary" @click="openPurchaseDialog">新建采购单</el-button>
    </div>

    <el-table :data="purchaseOrders" style="width: 100%" size="small">
      <el-table-column prop="poNo" label="采购单号" />

      <el-table-column label="供应商" min-width="160">
        <template #default="{ row }">
          {{
            (() => {
              const s = suppliers.find((ss) => ss.id === row.supplierId)
              return s?.supplierName ?? '-'
            })()
          }}
        </template>
      </el-table-column>

      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          {{ purchaseStatusLabel(row.status) }}
        </template>
      </el-table-column>

      <el-table-column prop="remark" label="备注" />

      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'CREATED'"
            link
            type="primary"
            size="small"
            @click="onMarkPurchased(row)"
          >
            购入
          </el-button>
          <span v-else-if="row.status === 'RECEIVING'">待仓库入库</span>
          <span v-else>已入库</span>

          <el-divider direction="vertical" />

          <el-button link type="primary" size="small" @click="openPurchaseDetailDialog(row)">
            详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建采购单 -->
    <el-dialog
      :model-value="showPurchaseDialog"
      title="新建采购单"
      width="560px"
      destroy-on-close
      @close="closePurchaseDialog"
    >
      <el-form label-width="100px" @submit.prevent>
        <el-form-item label="采购单号">
          <el-input v-model="purchaseForm.poNo" placeholder="如 PO20250306001" />
        </el-form-item>

        <el-form-item label="供应商">
          <el-select v-model="purchaseForm.supplierId" clearable placeholder="选择供应商" style="width: 100%">
            <el-option
              v-for="s in filteredSuppliers"
              :key="s.id"
              :label="s.supplierName"
              :value="s.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="purchaseForm.remark" type="textarea" rows="2" />
        </el-form-item>

        <el-form-item label="明细">
          <div style="width: 100%">
            <el-table :data="purchaseForm.lines" size="small" max-height="200">
              <el-table-column label="物料" min-width="140">
                <template #default="{ row }">
                  {{
                    (() => {
                      const m = materials.find((mm) => mm.id === row.materialId)
                      if (!m) return `ID:${row.materialId}`
                      return `${m.materialCode} ${m.materialName}${m.materialSpec ? ` (${m.materialSpec})` : ''}`
                    })()
                  }}
                </template>
              </el-table-column>

              <el-table-column label="数量" width="150">
                <template #header>
                  <span class="qty-header">数量</span>
                </template>
                <template #default="{ row }">
                  <div style="display: flex; justify-content: flex-start; padding-left: 0px;">
                    <el-input-number
                      v-model="row.qty"
                      :min="1"
                      :max="999"
                      :step="1"
                      size="small"
                      style="width: 90px"
                    />
                  </div>
                </template>
              </el-table-column>

              <el-table-column label="操作" width="60">
                <template #default="{ $index }">
                  <el-button link type="danger" size="small" @click="removePurchaseLine($index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <div style="display: flex; gap: 8px; margin-top: 8px; flex-wrap: wrap; align-items: flex-start">
              <el-select
                v-model="purchaseFormLine.materialId"
                placeholder="原材料"
                clearable
                filterable
                style="width: 200px"
              >
                <el-option
                  v-for="m in rawMaterialsForSupplier"
                  :key="m.id"
                  :label="`${m.materialCode} ${m.materialName}`"
                  :value="m.id"
                />
              </el-select>

              <el-input-number
                v-model="purchaseFormLine.qty"
                :min="1"
                :max="999"
                :step="1"
                placeholder="数量"
                style="width: 120px"
              />

              <el-button type="primary" size="small" @click="addPurchaseLine">添加行</el-button>
            </div>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="closePurchaseDialog">取消</el-button>
        <el-button
          type="primary"
          :loading="purchaseSubmitting"
          @click="submitPurchaseOrder"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 采购详情 -->
    <el-dialog
      :model-value="showPurchaseDetailDialog"
      :title="`采购单详情 - ${currentPurchaseOrder?.poNo || ''}`"
      width="600px"
      @close="closePurchaseDetailDialog"
    >
      <el-descriptions
        v-if="currentPurchaseOrder"
        :column="2"
        size="small"
        style="margin-bottom: 12px"
      >
        <el-descriptions-item label="采购单号">
          {{ currentPurchaseOrder.poNo }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ purchaseStatusLabel(currentPurchaseOrder.status) }}
        </el-descriptions-item>
        <el-descriptions-item label="供应商">
          {{
            (() => {
              const s = suppliers.find((ss) => ss.id === currentPurchaseOrder!.supplierId)
              return s?.supplierName ?? '-'
            })()
          }}
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">
          {{ currentPurchaseOrder.remark || '-' }}
        </el-descriptions-item>
      </el-descriptions>

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
                const m = materials.find((mm) => mm.id === row.materialId)
                if (!m) return `ID:${row.materialId}`
                return m.materialName
              })()
            }}
          </template>
        </el-table-column>

        <el-table-column prop="qty" label="采购数量" width="100" />
        <el-table-column prop="receivedQty" label="已入库数量" width="110" />
        <el-table-column prop="price" label="单价" width="90" />
        <el-table-column prop="remark" label="备注" />
      </el-table>

      <template #footer>
        <el-button @click="closePurchaseDetailDialog">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.action-bar {
  margin-bottom: 8px;
}

.qty-header {
  display: inline-block;
  transform: translateX(30px);
}
</style>

