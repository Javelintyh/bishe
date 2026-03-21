<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { ProductionWorkOrder } from '@/api/production'
import type { Material } from '@/api/base'
import type { BomItem } from '@/types/warehouse'
import { usePagination } from '@/composables/warehouse'
import { PAGE_SIZE, WORK_ORDER_STATUS_LABELS, type WorkOrderStatusCode } from '@/constants/warehouse'

const props = defineProps<{
  workOrders: ProductionWorkOrder[]
  materialMap: Record<number, Material>
  loading: boolean
  getWorkOrderBomItems: (wo: ProductionWorkOrder) => BomItem[]
  isWorkOrderStockSufficient: (wo: ProductionWorkOrder) => boolean
  getWorkOrderStockStatus: (wo: ProductionWorkOrder) => string
}>()

const emit = defineEmits<{
  quickOutbound: [wo: ProductionWorkOrder]
}>()

const workOrdersRef = computed(() => props.workOrders)
const { currentPage, pagedData, total, showPagination } = usePagination({
  data: workOrdersRef,
  pageSize: PAGE_SIZE,
})

const DUE_SOON_DAYS = 3

function getDueState(wo: ProductionWorkOrder): { tagText: string; tagType: 'danger' | 'warning' } | null {
  const dueDate = wo.dueDate
  if (!dueDate) return null

  const today = new Date()
  today.setHours(0, 0, 0, 0)

  const due = new Date(dueDate)
  due.setHours(0, 0, 0, 0)

  const diffDays = Math.floor((due.getTime() - today.getTime()) / (24 * 60 * 60 * 1000))
  if (diffDays < 0) {
    return { tagText: '超期', tagType: 'danger' }
  }
  if (diffDays <= DUE_SOON_DAYS) {
    return { tagText: `临期(${diffDays}天)`, tagType: 'warning' }
  }
  return null
}

function getDueDateText(wo: ProductionWorkOrder): string {
  return wo.dueDate ?? '-'
}

function getMaterialInfo(materialId: number): string {
  const m = props.materialMap[materialId]
  if (!m) return `ID:${materialId}`
  return `${m.materialName}${m.materialSpec ? ` (${m.materialSpec})` : ''}`
}

function getMaterialName(materialId: number): string {
  return props.materialMap[materialId]?.materialName ?? `ID:${materialId}`
}

function workOrderStatusLabel(status?: string | null): string {
  if (!status) return ''
  return WORK_ORDER_STATUS_LABELS[status as WorkOrderStatusCode] ?? status
}
</script>

<template>
  <el-table :data="pagedData" v-loading="loading" border stripe>
    <el-table-column label="工单号" prop="workOrderNo" />
    <el-table-column label="产品">
      <template #default="{ row }">
        <span>
          {{ getMaterialInfo(row.productMaterialId) }}
          <span v-if="row.urgent" class="urgent-workorder-label">（急）</span>
        </span>
      </template>
    </el-table-column>
    <el-table-column label="需求数量" prop="qty" />
    <el-table-column label="操作" width="120">
      <template #default="{ row }">
        <div v-if="getDueState(row)" style="margin-bottom: 6px;">
          <el-tag
            :type="getDueState(row)?.tagType"
            size="small"
            effect="plain"
          >
            {{ getDueState(row)?.tagText }}
          </el-tag>
        </div>
        <el-button
          :type="getDueState(row)?.tagType === 'danger' ? 'danger' : getDueState(row)?.tagType === 'warning' ? 'warning' : 'primary'"
          size="small"
          :disabled="!isWorkOrderStockSufficient(row)"
          @click="
            () => {
              const state = getDueState(row)
              if (state) ElMessage.warning(`该工单${state.tagText}，建议优先“一键出库”`)
              emit('quickOutbound', row)
            }
          "
        >
          一键出库
        </el-button>
      </template>
    </el-table-column>
    <el-table-column label="状态">
      <template #default="{ row }">
        {{ workOrderStatusLabel(row.status) }}
      </template>
    </el-table-column>
    <el-table-column label="交货日期" width="140">
      <template #default="{ row }">
        {{ getDueDateText(row) }}
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
  </el-table>
  <el-pagination
    v-if="showPagination"
    v-model:current-page="currentPage"
    :page-size="PAGE_SIZE"
    :total="total"
    layout="prev, pager, next"
    style="margin-top: 12px; justify-content: flex-end"
  />
</template>

<style scoped>
.urgent-workorder-label {
  margin-left: 6px;
  color: #f56c6c;
  font-weight: 600;
}
</style>
