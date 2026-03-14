<script setup lang="ts">
import { computed, ref } from 'vue'
import type * as inventoryApi from '@/api/inventory'
import type * as baseApi from '@/api/base'
import { materialTypeLabel } from '@/constants/admin'

const PAGE_SIZE = 5
const currentPage = ref(1)

const props = defineProps<{
  stocks: inventoryApi.InventoryStock[]
  materials: baseApi.Material[]
}>()

const pagedStocks = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return props.stocks.slice(start, start + PAGE_SIZE)
})

const materialMap = computed(() => new Map(props.materials.map(m => [m.id, m])))

function getMaterial(id: number | undefined) {
  if (!id) return null
  return materialMap.value.get(id) ?? null
}
</script>

<template>
  <div>
    <el-table :data="pagedStocks" style="width: 100%" size="small">
      <el-table-column label="物料编码" width="120">
        <template #default="{ row }">
          {{ getMaterial(row.materialId)?.materialCode ?? row.materialId }}
        </template>
      </el-table-column>
      <el-table-column label="物料名称" min-width="140">
        <template #default="{ row }">
          {{ getMaterial(row.materialId)?.materialName ?? '-' }}
        </template>
      </el-table-column>
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          {{ materialTypeLabel(getMaterial(row.materialId)?.materialType) }}
        </template>
      </el-table-column>
      <el-table-column label="单位" width="70">
        <template #default="{ row }">
          {{ getMaterial(row.materialId)?.unit ?? '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="qty" label="库存数量" width="100" />
      <el-table-column prop="updatedAt" label="更新时间" width="170" />
    </el-table>
    <el-pagination
      v-if="props.stocks.length > PAGE_SIZE"
      v-model:current-page="currentPage"
      :page-size="PAGE_SIZE"
      :total="props.stocks.length"
      layout="prev, pager, next"
      style="margin-top: 12px; justify-content: flex-end"
    />
  </div>
</template>

