<script setup lang="ts">
import type { MessageNotice } from '@/api/message'

const props = defineProps<{
  messages: MessageNotice[]
  loading: boolean
  onAcknowledge: (msg: MessageNotice) => void
}>()
</script>

<template>
  <el-table :data="props.messages" size="small" :loading="props.loading">
    <el-table-column prop="title" label="标题" width="140" />
    <el-table-column label="对象" width="220">
      <template #default="{ row }">
        {{ row.relatedId || '-' }}
      </template>
    </el-table-column>
    <el-table-column label="详情">
      <template #default="{ row }">
        <div v-for="(line, idx) in String(row.content || '').split('\n')" :key="idx">{{ line }}</div>
      </template>
    </el-table-column>
    <el-table-column prop="createdAt" label="时间" width="170" />
    <el-table-column label="操作" width="120">
      <template #default="{ row }">
        <el-button link type="primary" size="small" @click="props.onAcknowledge(row)">我知道了</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

