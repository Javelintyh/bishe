<script setup lang="ts">
import { computed, ref } from 'vue'
import type * as inventoryApi from '@/api/inventory'
import type * as baseApi from '@/api/base'
import { materialTypeLabel } from '@/constants/admin'

const PAGE_SIZE = 5
const currentPage = ref(1)
const typeFilter = ref<'ALL' | 'RAW' | 'PRODUCT'>('ALL')

const props = defineProps<{
  stocks: inventoryApi.InventoryStock[]
  materials: baseApi.Material[]
}>()

const materialMap = computed(() => new Map(props.materials.map(m => [m.id, m])))

const filteredStocks = computed(() => {
  if (typeFilter.value === 'ALL') {
    return props.stocks
  }
  return props.stocks.filter((stock) => {
    const material = getMaterial(stock.materialId)
    return material?.materialType === typeFilter.value
  })
})

const pagedStocks = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return filteredStocks.value.slice(start, start + PAGE_SIZE)
})

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
      <el-table-column label="类型" width="140">
        <template #header>
          <div class="type-header">
            <span>类型</span>
            <el-select
              v-model="typeFilter"
              size="small"
              class="type-select"
            >
              <el-option label="全部" value="ALL" />
              <el-option label="原材料" value="RAW" />
              <el-option label="成品" value="PRODUCT" />
            </el-select>
          </div>
        </template>
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
      v-if="filteredStocks.length > PAGE_SIZE"
      v-model:current-page="currentPage"
      :page-size="PAGE_SIZE"
      :total="filteredStocks.length"
      layout="prev, pager, next"
      style="margin-top: 12px; justify-content: flex-end"
    />
  </div>
</template>

<style scoped>
.type-header {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap
}

.type-select {
  width: 100px;
}

/* 美化下拉框 */
.type-select :deep(.el-select__wrapper) {
  background-color: #f9f9f9;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
  transition: all 0.3s ease;
  padding: 4px 12px;
  min-height: 32px;
}

.type-select :deep(.el-select__wrapper:hover) {
  background-color: #ffffff;
  border-color: #c0c4cc;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.type-select :deep(.el-select__wrapper.is-focused) {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64,158,255,0.2);
}

/* 美化下拉箭头 */
.type-select :deep(.el-select__caret) {
  color: #909399;
  font-size: 14px;
  transition: transform 0.3s;
}

.type-select :deep(.el-select__caret.is-reverse) {
  transform: rotate(180deg);
}

/* 下拉菜单样式 */
.type-select :deep(.el-select-dropdown) {
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  margin-top: 4px;
}

.type-select :deep(.el-select-dropdown__item) {
  border-radius: 4px;
  margin: 4px 8px;
  padding: 8px 12px;
  height: auto;
  line-height: 1.4;
}

.type-select :deep(.el-select-dropdown__item.selected) {
  background-color: #ecf5ff;
  color: #409eff;
  font-weight: 500;
}

.type-select :deep(.el-select-dropdown__item.hover) {
  background-color: #f5f7fa;
}
</style>
