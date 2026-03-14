<script setup lang="ts">
import { computed } from 'vue'
import type { ProductionWorkOrder } from '@/api/production'
import type { Material } from '@/api/base'
import { usePagination } from '@/composables/warehouse'
import { PAGE_SIZE } from '@/constants/warehouse'

const props = defineProps<{
  workOrders: ProductionWorkOrder[]
  materialMap: Record<number, Material>
  loading: boolean
}>()

const emit = defineEmits<{
  quickInbound: [wo: ProductionWorkOrder]
}>()

const workOrdersRef = computed(() => props.workOrders)
const { currentPage, pagedData, total, showPagination } = usePagination({
  data: workOrdersRef,
  pageSize: PAGE_SIZE,
})

function getMaterialInfo(materialId: number): string {
  const m = props.materialMap[materialId]
  if (!m) return `ID:${materialId}`
  return `${m.materialName}${m.materialSpec ? ` (${m.materialSpec})` : ''}`
}
</script>

<template>
  <el-table :data="pagedData" v-loading="loading" border stripe>
    <el-table-column label="工单号" prop="workOrderNo" />
    <el-table-column label="产品">
      <template #default="{ row }">
        {{ getMaterialInfo(row.productMaterialId) }}
      </template>
    </el-table-column>
    <el-table-column label="生产数量" prop="qty" />
    <el-table-column label="操作" width="120">
      <template #default="{ row }">
        <el-button type="success" size="small" @click="emit('quickInbound', row)">
          一键入库
        </el-button>
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
