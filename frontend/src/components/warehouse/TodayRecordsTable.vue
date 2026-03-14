<script setup lang="ts">
import { computed } from 'vue'
import type { InventoryRecord } from '@/api/inventory'
import type { Material } from '@/api/base'
import { usePagination } from '@/composables/warehouse'
import { PAGE_SIZE, BIZ_TYPE_LABELS, type BizTypeCode } from '@/constants/warehouse'

const props = defineProps<{
  records: InventoryRecord[]
  materialMap: Record<number, Material>
  loading: boolean
}>()

const recordsRef = computed(() => props.records)
const { currentPage, pagedData, total, showPagination } = usePagination({
  data: recordsRef,
  pageSize: PAGE_SIZE,
})

function getMaterialInfo(materialId: number): string {
  const m = props.materialMap[materialId]
  if (!m) return `ID:${materialId}`
  return `${m.materialName}${m.materialSpec ? ` (${m.materialSpec})` : ''}`
}

function bizTypeLabel(type?: string | null): string {
  if (!type) return ''
  return BIZ_TYPE_LABELS[type as BizTypeCode] ?? type
}
</script>

<template>
  <el-table :data="pagedData" v-loading="loading" border stripe>
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
    v-if="showPagination"
    v-model:current-page="currentPage"
    :page-size="PAGE_SIZE"
    :total="total"
    layout="prev, pager, next"
    style="margin-top: 12px; justify-content: flex-end"
  />
</template>
