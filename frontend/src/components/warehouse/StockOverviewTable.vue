<script setup lang="ts">
import { computed, ref } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import type { InventoryStock } from '@/api/inventory'
import type { Material } from '@/api/base'
import { usePagination } from '@/composables/warehouse'
import { PAGE_SIZE } from '@/constants/warehouse'

const props = defineProps<{
  stocks: InventoryStock[]
  materialMap: Record<number, Material>
  loading: boolean
}>()

const searchKeyword = ref('')
const typeFilter = ref<'ALL' | 'RAW' | 'PRODUCT'>('ALL')
const qtySortOrder = ref<'ascending' | 'descending' | null>(null)

const filteredStocks = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  return props.stocks.filter((stock) => {
    const material = props.materialMap[stock.materialId]

    // 按类型筛选
    if (typeFilter.value !== 'ALL') {
      if (!material || material.materialType !== typeFilter.value) {
        return false
      }
    }

    // 没有关键字时不过滤文本
    if (!keyword) {
      return true
    }

    const code = material?.materialCode?.toLowerCase() ?? ''
    const name = material?.materialName?.toLowerCase() ?? ''
    const spec = material?.materialSpec?.toLowerCase() ?? ''
    const idStr = String(stock.materialId)
    return (
      code.includes(keyword) ||
      name.includes(keyword) ||
      spec.includes(keyword) ||
      idStr.includes(keyword)
    )
  })
})

const sortedStocks = computed(() => {
  if (!qtySortOrder.value) return filteredStocks.value

  const arr = [...filteredStocks.value]
  arr.sort((a, b) => {
    const aq = Number(a.qty)
    const bq = Number(b.qty)
    if (qtySortOrder.value === 'ascending') return aq - bq
    return bq - aq
  })
  return arr
})

const stocksRef = computed(() => sortedStocks.value)
const { currentPage, pagedData, total, showPagination, resetPage } = usePagination({
  data: stocksRef,
  pageSize: PAGE_SIZE,
})

function getMaterialInfo(materialId: number): string {
  const m = props.materialMap[materialId]
  if (!m) return `ID:${materialId}`
  return `${m.materialName}${m.materialSpec ? ` (${m.materialSpec})` : ''}`
}

function onQtySortChange(params: any) {
  const order = params?.order as 'ascending' | 'descending' | null | undefined
  qtySortOrder.value = order ?? null
  resetPage()
}

</script>

<template>
  <div>
    <el-input
      v-model="searchKeyword"
      placeholder="请输入物料编码 / 名称 / 规格 "
      clearable
      size="small"
      style="max-width: 320px; margin-bottom: 8px"
    />
    <el-table
      :data="pagedData"
      v-loading="loading"
      border
      stripe
      @sort-change="onQtySortChange"
    >
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
    <el-table-column label="类型" min-width="140">
      <template #header>
        <div class="type-header">
          <span>类型</span>
          <el-select
            v-model="typeFilter"
            size="small"
            class="type-select"
            :suffix-icon="ArrowDown"
          >
            <el-option label="全部" value="ALL" />
            <el-option label="原材料" value="RAW" />
            <el-option label="成品" value="PRODUCT" />
          </el-select>
        </div>
      </template>
      <template #default="{ row }">
        {{ materialMap[row.materialId]?.materialType === 'RAW' ? '原材料' : '成品' }}
      </template>
    </el-table-column>
    <el-table-column
      label="库存数量"
      prop="qty"
      sortable="custom"
      :sort-orders="['ascending', 'descending']"
    />
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
