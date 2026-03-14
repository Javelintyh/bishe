<script setup lang="ts">
import { computed } from 'vue'
import type { ProductionReport } from '@/api/production'
import { usePagination } from '@/composables/workshop'
import { PAGE_SIZE } from '@/constants/workshop'

const props = defineProps<{
  reports: ProductionReport[]
  loading: boolean
}>()

const reportsRef = computed(() => props.reports)
const { currentPage, pagedData, total, showPagination } = usePagination({
  data: reportsRef,
  pageSize: PAGE_SIZE,
})
</script>

<template>
  <el-table :data="pagedData" v-loading="loading" border stripe>
    <el-table-column label="工单ID" prop="workOrderId" />
    <el-table-column label="工序/成品" prop="processName" />
    <el-table-column label="良品数" prop="goodQty" />
    <el-table-column label="不良数" prop="badQty" />
    <el-table-column label="不良原因" prop="badReasonCode" />
    <el-table-column label="报工时间" prop="reportTime" />
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
