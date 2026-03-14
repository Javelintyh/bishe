<script setup lang="ts">
import { computed, ref } from 'vue'
import type * as productionApi from '@/api/production'
import type * as orderApi from '@/api/order'
import type * as baseApi from '@/api/base'
import { workOrderStatusLabel } from './helpers'

const PAGE_SIZE = 5
const currentPage = ref(1)

const props = defineProps<{
  workOrders: productionApi.ProductionWorkOrder[]
  orders: orderApi.OrderVO[]
  materials: baseApi.Material[]
}>()

const pagedWorkOrders = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return props.workOrders.slice(start, start + PAGE_SIZE)
})

const orderMap = computed(() => new Map(props.orders.map(o => [o.id, o])))
const materialMap = computed(() => new Map(props.materials.map(m => [m.id, m])))

function getOrderNo(id: number | undefined) {
  if (!id) return '-'
  const o = orderMap.value.get(id)
  return o ? o.orderNo : String(id)
}

function getMaterialName(id: number | undefined) {
  if (!id) return '-'
  const m = materialMap.value.get(id)
  return m ? `${m.materialCode} / ${m.materialName}` : String(id)
}
</script>

<template>
  <div>
    <el-table :data="pagedWorkOrders" style="width: 100%" size="small">
      <el-table-column prop="workOrderNo" label="工单号" width="140" />
      <el-table-column label="关联订单" width="150">
        <template #default="{ row }">
          {{ getOrderNo(row.orderId) }}
        </template>
      </el-table-column>
      <el-table-column label="产品" min-width="180">
        <template #default="{ row }">
          {{ getMaterialName(row.productMaterialId) }}
        </template>
      </el-table-column>
      <el-table-column prop="qty" label="数量" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          {{ workOrderStatusLabel(row.status) }}
        </template>
      </el-table-column>
      <el-table-column prop="dueDate" label="计划完成" width="110" />
    </el-table>
    <el-pagination
      v-if="props.workOrders.length > PAGE_SIZE"
      v-model:current-page="currentPage"
      :page-size="PAGE_SIZE"
      :total="props.workOrders.length"
      layout="prev, pager, next"
      style="margin-top: 12px; justify-content: flex-end"
    />
  </div>
</template>

