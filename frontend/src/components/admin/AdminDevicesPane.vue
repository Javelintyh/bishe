<script setup lang="ts">
import { computed, ref } from 'vue'
import type * as deviceApi from '@/api/device'
import { deviceStatusLabel } from '@/constants/admin'

const PAGE_SIZE = 5
const currentPage = ref(1)

const props = defineProps<{
  devices: deviceApi.DeviceAsset[]
}>()

const pagedDevices = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return props.devices.slice(start, start + PAGE_SIZE)
})
</script>

<template>
  <div>
    <el-table :data="pagedDevices" style="width: 100%" size="small">
      <el-table-column prop="deviceCode" label="设备编码" />
      <el-table-column prop="deviceName" label="设备名称" />
      <el-table-column prop="model" label="型号" />
      <el-table-column prop="location" label="位置" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          {{ deviceStatusLabel(row.status) }}
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" />
    </el-table>
    <el-pagination
      v-if="props.devices.length > PAGE_SIZE"
      v-model:current-page="currentPage"
      :page-size="PAGE_SIZE"
      :total="props.devices.length"
      layout="prev, pager, next"
      style="margin-top: 12px; justify-content: flex-end"
    />
  </div>
</template>

