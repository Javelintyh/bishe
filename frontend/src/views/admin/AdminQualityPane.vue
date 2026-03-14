<script setup lang="ts">
import { computed, ref } from 'vue'
import type * as qualityApi from '@/api/quality'

const PAGE_SIZE = 5
const currentPage = ref(1)

const props = defineProps<{
  reasons: qualityApi.QualityReason[]
}>()

const pagedReasons = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return props.reasons.slice(start, start + PAGE_SIZE)
})
</script>

<template>
  <div>
    <el-table :data="pagedReasons" style="width: 100%" size="small">
      <el-table-column prop="reasonCode" label="原因编码" />
      <el-table-column prop="reasonName" label="原因名称" />
      <el-table-column prop="enabled" label="启用" width="80">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortNo" label="排序" width="80" />
    </el-table>
    <el-pagination
      v-if="props.reasons.length > PAGE_SIZE"
      v-model:current-page="currentPage"
      :page-size="PAGE_SIZE"
      :total="props.reasons.length"
      layout="prev, pager, next"
      style="margin-top: 12px; justify-content: flex-end"
    />
  </div>
</template>

