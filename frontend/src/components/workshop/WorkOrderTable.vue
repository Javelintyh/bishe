<script setup lang="ts">
import { computed } from 'vue'
import type { ProductionWorkOrder } from '@/api/production'
import { usePagination } from '@/composables/workshop'
import { PAGE_SIZE, WORK_ORDER_STATUS_LABELS, type WorkOrderStatusCode } from '@/constants/workshop'

const props = defineProps<{
  workOrders: ProductionWorkOrder[]
  loading: boolean
  getMaterialName: (id: number) => string
  getProductBomInfo: (productMaterialId: number) => string
}>()

const emit = defineEmits<{
  goToReport: [wo: ProductionWorkOrder]
}>()

const workOrdersRef = computed(() => props.workOrders)
const { currentPage, pagedData, total, showPagination } = usePagination({
  data: workOrdersRef,
  pageSize: PAGE_SIZE,
})

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
        {{ getMaterialName(row.productMaterialId) }}
      </template>
    </el-table-column>
    <el-table-column label="需生产数量" prop="qty" />
    <el-table-column label="BOM配方" min-width="200">
      <template #default="{ row }">
        {{ getProductBomInfo(row.productMaterialId) }}
      </template>
    </el-table-column>
    <el-table-column label="状态">
      <template #default="{ row }">
        {{ workOrderStatusLabel(row.status) }}
      </template>
    </el-table-column>
    <el-table-column label="操作" width="100">
      <template #default="{ row }">
        <el-button type="primary" size="small" @click="emit('goToReport', row)">报工</el-button>
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
