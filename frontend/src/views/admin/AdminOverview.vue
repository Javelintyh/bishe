<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, TitleComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { InventoryTurnoverDTO, OrderSummaryDTO, ProductionSummaryDTO } from '@/api/report'

use([BarChart, LineChart, GridComponent, TooltipComponent, TitleComponent, CanvasRenderer])

const props = defineProps<{
  ordersCount: number
  workOrdersCount: number
  materialsCount: number
  orderSummary: OrderSummaryDTO | null
  productionSummary: ProductionSummaryDTO | null
  inventoryTurnover: InventoryTurnoverDTO | null
}>()

const emit = defineEmits<{
  (e: 'export-report'): void
}>()

const chartOption = computed(() => {
  const os = props.orderSummary
  const ps = props.productionSummary
  const inv = props.inventoryTurnover
  const totalOrders = os?.totalCount ?? 0
  const onTimeRate = totalOrders ? ((os?.onTimeDeliveredCount ?? 0) / totalOrders) * 100 : 0
  const totalWo = ps?.totalWorkOrders ?? 0
  const doneRate = totalWo ? ((ps?.doneWorkOrders ?? 0) / totalWo) * 100 : 0
  return {
    title: { text: '核心指标概览', left: 'center' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['订单准时交付率', '生产完成率', '近30天出库量'] },
    yAxis: [
      { type: 'value', name: '比率(%)', min: 0, max: 100, position: 'left', axisLabel: { formatter: '{value}%' } },
      { type: 'value', name: '出库量', position: 'right' },
    ],
    series: [
      {
        name: '比率(%)',
        type: 'bar',
        data: [onTimeRate, doneRate, null],
        itemStyle: { color: '#409eff' },
        yAxisIndex: 0,
        barWidth: 42,
        barGap: '-100%',
        label: {
          show: true,
          position: 'inside',
          align: 'center',
          verticalAlign: 'middle',
          color: '#fff',
          formatter: ({ value }: { value: number | null }) => (value == null ? '' : `${Number(value).toFixed(1)}%`),
        },
      },
      {
        name: '出库量',
        type: 'bar',
        data: [null, null, inv?.totalOutQty ?? 0],
        itemStyle: { color: '#67c23a' },
        yAxisIndex: 1,
        barWidth: 42,
        barGap: '-100%',
        label: {
          show: true,
          position: 'inside',
          align: 'center',
          verticalAlign: 'middle',
          color: '#fff',
          formatter: ({ value }: { value: number | null }) => (value == null ? '' : String(value)),
        },
      },
    ],
  }
})
</script>

<template>
  <div>
    <div class="metric-row">
      <el-card class="metric-card" style="flex: 1">
        <div>订单数量</div>
        <h2>{{ ordersCount }}</h2>
      </el-card>
      <el-card class="metric-card" style="flex: 1">
        <div>工单数量</div>
        <h2>{{ workOrdersCount }}</h2>
      </el-card>
      <el-card class="metric-card" style="flex: 1">
        <div>物料种类</div>
        <h2>{{ materialsCount }}</h2>
      </el-card>
    </div>
    <div v-if="orderSummary && productionSummary && inventoryTurnover" class="metric-row">
      <el-card class="metric-card" style="flex: 1">
        <div>订单准时交付率</div>
        <h2>
          {{
            orderSummary.totalCount
              ? ((orderSummary.onTimeDeliveredCount / orderSummary.totalCount) * 100).toFixed(1)
              : '0.0'
          }}%
        </h2>
      </el-card>
      <el-card class="metric-card" style="flex: 1">
        <div>生产完成率</div>
        <h2>
          {{
            productionSummary.totalWorkOrders
              ? ((productionSummary.doneWorkOrders / productionSummary.totalWorkOrders) * 100).toFixed(1)
              : '0.0'
          }}%
        </h2>
      </el-card>
      <el-card class="metric-card" style="flex: 1">
        <div>近30天出库总量</div>
        <h2>{{ inventoryTurnover.totalOutQty }}</h2>
      </el-card>
    </div>
    <div style="margin-top: 16px">
      <el-button size="small" class="soft-btn" @click="emit('export-report')">导出报表 CSV</el-button>
    </div>
    <div class="chart-wrap">
      <v-chart :option="chartOption" style="width: 100%; height: 100%" autoresize />
    </div>
    <p class="tip-text">可在下方各个 Tab 中维护详细数据。</p>
  </div>
</template>

<style scoped>
.metric-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.metric-card {
  border: 0;
  border-radius: 12px;
  box-shadow: inset 0 0 0 1px #e7efff;
  background: linear-gradient(180deg, #f8fbff 0%, #eef4ff 100%);
}

.metric-card h2 {
  margin: 10px 0 0;
  color: #2f3a50;
}

.chart-wrap {
  height: 280px;
  margin-top: 12px;
  border-radius: 12px;
  padding: 8px;
  background: #fff;
  box-shadow: inset 0 0 0 1px #e7efff;
}

.tip-text {
  margin-top: 12px;
  color: #909399;
}

.soft-btn {
  border-radius: 10px;
}
</style>
