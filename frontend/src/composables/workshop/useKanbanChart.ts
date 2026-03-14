/**
 * 看板图表组合式函数
 */
import { ref } from 'vue'
import * as echarts from 'echarts'
import type { ProductionWorkOrder } from '@/api/production'
import { WORK_ORDER_STATUS_LABELS, KANBAN_CHART_COLORS } from '@/constants/workshop'

/**
 * Notes:
 * - 工单状态分布饼图逻辑封装
 *
 * Returns:
 * - kanbanChartRef: 图表容器引用
 * - renderKanban: 渲染看板图表
 * - disposeChart: 销毁图表
 */
export function useKanbanChart() {
  const kanbanChartRef = ref<HTMLElement | null>(null)
  let chartInstance: echarts.ECharts | null = null

  function renderKanban(allWorkOrders: ProductionWorkOrder[]) {
    if (!kanbanChartRef.value) return

    if (chartInstance) {
      chartInstance.dispose()
    }
    chartInstance = echarts.init(kanbanChartRef.value)

    const statusCount: Record<string, number> = {
      TO_PRODUCE: 0,
      PRODUCING: 0,
      DONE: 0,
      STORED: 0,
    }
    for (const wo of allWorkOrders) {
      statusCount[wo.status] = (statusCount[wo.status] || 0) + 1
    }

    chartInstance.setOption({
      title: { text: '工单状态分布', left: 'center' },
      tooltip: { trigger: 'item' },
      color: KANBAN_CHART_COLORS,
      series: [
        {
          type: 'pie',
          radius: '60%',
          data: [
            { value: statusCount.TO_PRODUCE, name: WORK_ORDER_STATUS_LABELS.TO_PRODUCE },
            { value: statusCount.PRODUCING, name: WORK_ORDER_STATUS_LABELS.PRODUCING },
            { value: statusCount.DONE, name: WORK_ORDER_STATUS_LABELS.DONE },
            { value: statusCount.STORED, name: WORK_ORDER_STATUS_LABELS.STORED },
          ],
        },
      ],
    })
  }

  function disposeChart() {
    if (chartInstance) {
      chartInstance.dispose()
      chartInstance = null
    }
  }

  return {
    kanbanChartRef,
    renderKanban,
    disposeChart,
  }
}
