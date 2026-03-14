<script setup lang="ts">
import { onMounted, onUnmounted, watch } from 'vue'
import type { ProductionWorkOrder } from '@/api/production'
import { useKanbanChart } from '@/composables/workshop'

const props = defineProps<{
  allWorkOrders: ProductionWorkOrder[]
}>()

const { kanbanChartRef, renderKanban, disposeChart } = useKanbanChart()

onMounted(() => {
  if (props.allWorkOrders.length > 0) {
    renderKanban(props.allWorkOrders)
  }
})

watch(
  () => props.allWorkOrders,
  (newVal) => {
    if (newVal.length > 0) {
      renderKanban(newVal)
    }
  },
)

onUnmounted(() => {
  disposeChart()
})
</script>

<template>
  <div ref="kanbanChartRef" style="width: 100%; height: 320px;"></div>
</template>
