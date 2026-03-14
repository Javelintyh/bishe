<script setup lang="ts">
import { computed } from 'vue'
import type { OrderVO } from '@/api/order'
import type { Material } from '@/api/base'
import { usePagination } from '@/composables/warehouse'
import { PAGE_SIZE, ORDER_STATUS_LABELS, type OrderStatusCode } from '@/constants/warehouse'

const props = defineProps<{
  orders: OrderVO[]
  materialMap: Record<number, Material>
  loading: boolean
}>()

const emit = defineEmits<{
  shipOrder: [order: OrderVO]
}>()

const ordersRef = computed(() => props.orders)
const { currentPage, pagedData, total, showPagination } = usePagination({
  data: ordersRef,
  pageSize: PAGE_SIZE,
})

function getMaterialInfo(materialId: number | undefined): string {
  if (!materialId) return '-'
  const m = props.materialMap[materialId]
  if (!m) return `ID:${materialId}`
  return `${m.materialName}${m.materialSpec ? ` (${m.materialSpec})` : ''}`
}

function orderStatusLabel(status?: string | null): string {
  if (!status) return ''
  return ORDER_STATUS_LABELS[status as OrderStatusCode] ?? status
}
</script>

<template>
  <el-table :data="pagedData" v-loading="loading" border stripe>
    <el-table-column label="订单号" prop="orderNo" />
    <el-table-column label="产品">
      <template #default="{ row }">
        {{ getMaterialInfo(row.productMaterialId) }}
      </template>
    </el-table-column>
    <el-table-column label="数量" prop="qty" />
    <el-table-column label="状态">
      <template #default="{ row }">
        <el-tag type="success">{{ orderStatusLabel(row.status) }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="操作" width="120">
      <template #default="{ row }">
        <el-button type="warning" size="small" @click="emit('shipOrder', row)">
          发货
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
