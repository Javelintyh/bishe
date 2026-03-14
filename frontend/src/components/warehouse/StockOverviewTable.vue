<script setup lang="ts">
import { computed } from 'vue'
import type { InventoryStock } from '@/api/inventory'
import type { Material } from '@/api/base'
import { usePagination } from '@/composables/warehouse'
import { PAGE_SIZE } from '@/constants/warehouse'

const props = defineProps<{
  stocks: InventoryStock[]
  materialMap: Record<number, Material>
  loading: boolean
}>()

const stocksRef = computed(() => props.stocks)
const { currentPage, pagedData, total, showPagination } = usePagination({
  data: stocksRef,
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
    v-if="showPagination"
    v-model:current-page="currentPage"
    :page-size="PAGE_SIZE"
    :total="total"
    layout="prev, pager, next"
    style="margin-top: 12px; justify-content: flex-end"
  />
</template>
