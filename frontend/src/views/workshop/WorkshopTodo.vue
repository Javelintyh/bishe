<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as productionApi from '@/api/production'
import * as baseApi from '@/api/base'
import { workOrderStatusLabel } from '@/views/admin/helpers'
import * as echarts from 'echarts'

const PAGE_SIZE = 5

const loading = ref(false)
const state = reactive({
  workOrders: [] as productionApi.ProductionWorkOrder[],
  allWorkOrders: [] as productionApi.ProductionWorkOrder[],
  reports: [] as productionApi.ProductionReport[],
  reasons: [
    { code: 'MATERIAL', text: '原材料问题' },
    { code: 'EQUIPMENT', text: '设备故障' },
    { code: 'HUMAN', text: '人为失误' },
    { code: 'OTHER', text: '其他原因' },
  ],
  materials: [] as baseApi.Material[],
  bomMap: {} as Record<number, baseApi.BomLine[]>,
})

const materialMap = computed(() => {
  const m: Record<number, baseApi.Material> = {}
  for (const mat of state.materials) {
    m[mat.id] = mat
  }
  return m
})

const productMaterials = computed(() => state.materials.filter((m) => m.materialType === 'PRODUCT'))
const rawMaterials = computed(() => state.materials.filter((m) => m.materialType === 'RAW'))

function getMaterialName(id: number): string {
  return materialMap.value[id]?.materialName ?? `ID:${id}`
}

function getMaterialUnit(id: number): string {
  return materialMap.value[id]?.unit ?? ''
}

async function loadData() {
  loading.value = true
  try {
    const [woRes, allWoRes, reportsRes, materialsRes] = await Promise.all([
      productionApi.listWorkOrders({ status: 'PRODUCING' }),
      productionApi.listWorkOrders(),
      productionApi.listReports(),
      baseApi.listMaterials(),
    ])
    state.workOrders = woRes.data ?? []
    state.allWorkOrders = allWoRes.data ?? []
    state.reports = reportsRes.data ?? []
    state.materials = materialsRes.data ?? []

    const bomMapTemp: Record<number, baseApi.BomLine[]> = {}
    for (const prod of productMaterials.value) {
      const bomRes = await baseApi.listBom(prod.id)
      bomMapTemp[prod.id] = bomRes.data ?? []
    }
    state.bomMap = bomMapTemp

    renderKanban()
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

const kanbanChartRef = ref<HTMLElement | null>(null)

function renderKanban() {
  if (!kanbanChartRef.value) return
  const chart = echarts.init(kanbanChartRef.value)

  const statusCount: Record<string, number> = {
    TO_PRODUCE: 0,
    PRODUCING: 0,
    DONE: 0,
    STORED: 0,
  }
  for (const wo of state.allWorkOrders) {
    statusCount[wo.status] = (statusCount[wo.status] || 0) + 1
  }

  chart.setOption({
    title: { text: '工单状态分布', left: 'center' },
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: '60%',
        data: [
          { value: statusCount.TO_PRODUCE, name: '待生产' },
          { value: statusCount.PRODUCING, name: '生产中' },
          { value: statusCount.DONE, name: '已完成' },
          { value: statusCount.STORED, name: '已入库' },
        ],
      },
    ],
  })
}

function getProductBomInfo(productMaterialId: number): string {
  const bomLines = state.bomMap[productMaterialId] ?? []
  if (bomLines.length === 0) return '无BOM配置'
  return bomLines
    .map((line) => `${getMaterialName(line.materialId)} x${line.qty}`)
    .join(', ')
}

const reportableWorkOrders = computed(() =>
  state.workOrders.filter((w) => w.status === 'PRODUCING'),
)

const reportForm = reactive({
  workOrderNo: '',
  processName: '',
  goodQty: 1,
  badQty: 0,
  badReasonCode: '',
  badReasonText: '',
})

const selectedProductName = computed(() => {
  const wo = state.workOrders.find((w) => w.workOrderNo === reportForm.workOrderNo)
  if (!wo) return ''
  return getMaterialName(wo.productMaterialId)
})

function onSelectReportWorkOrder(workOrderNo: string) {
  const wo = state.workOrders.find((w) => w.workOrderNo === workOrderNo)
  if (wo) {
    reportForm.goodQty = wo.qty
    reportForm.processName = getMaterialName(wo.productMaterialId)
  }
}

function goToReport(wo: productionApi.ProductionWorkOrder) {
  activeTab.value = 'report'
  reportForm.workOrderNo = wo.workOrderNo
  reportForm.processName = getMaterialName(wo.productMaterialId)
  reportForm.goodQty = wo.qty
}

async function onReport() {
  if (!reportForm.workOrderNo) {
    ElMessage.warning('请选择工单')
    return
  }
  try {
    await productionApi.createReport({
      workOrderNo: reportForm.workOrderNo,
      processName: reportForm.processName,
      goodQty: reportForm.goodQty,
      badQty: reportForm.badQty,
      badReasonCode: reportForm.badReasonCode || undefined,
      badReasonText: reportForm.badReasonText || undefined,
    })

    const wo = state.workOrders.find((w) => w.workOrderNo === reportForm.workOrderNo)
    if (wo) {
      await productionApi.updateWorkOrderStatus(wo.id, 'DONE')
    }

    ElMessage.success('报工成功')
    reportForm.workOrderNo = ''
    reportForm.processName = ''
    reportForm.goodQty = 1
    reportForm.badQty = 0
    reportForm.badReasonCode = ''
    reportForm.badReasonText = ''
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '报工失败')
  }
}

const selectedProductId = ref<number | null>(null)
const bomLines = ref<Array<{ materialId: number | null; qty: number }>>([])
const bomSubmitting = ref(false)

function onSelectProduct(productId: number) {
  selectedProductId.value = productId
  const existingBom = state.bomMap[productId] ?? []
  if (existingBom.length > 0) {
    bomLines.value = existingBom.map((line) => ({
      materialId: line.materialId,
      qty: line.qty,
    }))
  } else {
    bomLines.value = [{ materialId: null, qty: 1 }]
  }
}

function addBomLine() {
  bomLines.value.push({ materialId: null, qty: 1 })
}

function removeBomLine(index: number) {
  bomLines.value.splice(index, 1)
}

async function saveBom() {
  if (!selectedProductId.value) {
    ElMessage.warning('请选择成品')
    return
  }
  const validLines = bomLines.value.filter((line) => line.materialId !== null && line.qty > 0)
  if (validLines.length === 0) {
    ElMessage.warning('请至少添加一条BOM配方')
    return
  }

  bomSubmitting.value = true
  try {
    await baseApi.bulkSetBom({
      productMaterialId: selectedProductId.value,
      lines: validLines.map((line) => ({
        materialId: line.materialId!,
        qty: line.qty,
      })),
    })
    ElMessage.success('BOM配方保存成功')
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  } finally {
    bomSubmitting.value = false
  }
}

const activeTab = ref('kanban')

const workOrdersPage = ref(1)
const pagedWorkOrders = computed(() => {
  const start = (workOrdersPage.value - 1) * PAGE_SIZE
  return reportableWorkOrders.value.slice(start, start + PAGE_SIZE)
})

const reportsPage = ref(1)
const pagedReports = computed(() => {
  const start = (reportsPage.value - 1) * PAGE_SIZE
  return state.reports.slice(start, start + PAGE_SIZE)
})
</script>

<template>
  <div class="workshop-todo">
    <el-card class="workshop-card">
      <template #header>
        <div class="page-header">
          <div>
            <div class="page-title">车间看板</div>
            <div class="page-subtitle">生产工单、报工管理</div>
          </div>
          <el-button class="soft-btn" type="primary" :loading="loading" @click="loadData">刷新数据</el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab" tab-position="left" class="page-tabs" style="min-height: 420px">
        <el-tab-pane label="生产看板" name="kanban">
          <div ref="kanbanChartRef" style="width: 100%; height: 320px;"></div>
        </el-tab-pane>

        <el-tab-pane name="pending">
          <template #label>
            <el-badge :value="reportableWorkOrders.length" :hidden="reportableWorkOrders.length === 0" :max="99" class="tab-badge">
              <span>待生产工单</span>
            </el-badge>
          </template>
          <el-table :data="pagedWorkOrders" v-loading="loading" border stripe>
            <el-table-column label="工单号" prop="workOrderNo" />
            <el-table-column label="产品">
              <template #default="{ row }">
                {{ getMaterialName(row.productMaterialId) }}
              </template>
            </el-table-column>
            <el-table-column label="需生产数量" prop="qty" />
            <el-table-column label="BOM配方" min-width="200">
              <template #default="{ row }">
                {{ getProductBomInfo(row.productMaterialId) }}
              </template>
            </el-table-column>
            <el-table-column label="状态">
              <template #default="{ row }">
                {{ workOrderStatusLabel(row.status) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="goToReport(row)">报工</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="reportableWorkOrders.length > PAGE_SIZE"
            v-model:current-page="workOrdersPage"
            :page-size="PAGE_SIZE"
            :total="reportableWorkOrders.length"
            layout="prev, pager, next"
            style="margin-top: 12px; justify-content: flex-end"
          />
        </el-tab-pane>

        <el-tab-pane label="生产报工" name="report">
          <el-form :model="reportForm" label-width="120px" style="max-width: 500px;">
            <el-form-item label="工单号">
              <el-select
                v-model="reportForm.workOrderNo"
                placeholder="选择工单"
                filterable
                @change="onSelectReportWorkOrder"
              >
                <el-option
                  v-for="wo in reportableWorkOrders"
                  :key="wo.workOrderNo"
                  :label="`${wo.workOrderNo} - ${getMaterialName(wo.productMaterialId)} (需生产${wo.qty})`"
                  :value="wo.workOrderNo"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="加工成品">
              <el-input v-model="reportForm.processName" disabled />
            </el-form-item>
            <el-form-item label="良品数量">
              <el-input-number v-model="reportForm.goodQty" :min="0" />
            </el-form-item>
            <el-form-item label="不良数量">
              <el-input-number v-model="reportForm.badQty" :min="0" />
            </el-form-item>
            <el-form-item label="不良原因" v-if="reportForm.badQty > 0">
              <el-select v-model="reportForm.badReasonCode" placeholder="选择原因">
                <el-option
                  v-for="r in state.reasons"
                  :key="r.code"
                  :label="r.text"
                  :value="r.code"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="原因说明" v-if="reportForm.badQty > 0">
              <el-input v-model="reportForm.badReasonText" type="textarea" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="onReport">提交报工</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="报工记录" name="history">
          <el-table :data="pagedReports" v-loading="loading" border stripe>
            <el-table-column label="工单ID" prop="workOrderId" />
            <el-table-column label="工序/成品" prop="processName" />
            <el-table-column label="良品数" prop="goodQty" />
            <el-table-column label="不良数" prop="badQty" />
            <el-table-column label="不良原因" prop="badReasonCode" />
            <el-table-column label="报工时间" prop="reportTime" />
          </el-table>
          <el-pagination
            v-if="state.reports.length > PAGE_SIZE"
            v-model:current-page="reportsPage"
            :page-size="PAGE_SIZE"
            :total="state.reports.length"
            layout="prev, pager, next"
            style="margin-top: 12px; justify-content: flex-end"
          />
        </el-tab-pane>

        <el-tab-pane label="BOM 配方" name="bom">
          <el-form label-width="120px" style="max-width: 600px;">
            <el-form-item label="选择成品">
              <el-select
                v-model="selectedProductId"
                placeholder="选择成品"
                filterable
                @change="onSelectProduct"
              >
                <el-option
                  v-for="m in productMaterials"
                  :key="m.id"
                  :label="`${m.materialName}${m.materialSpec ? ` (${m.materialSpec})` : ''}`"
                  :value="m.id"
                />
              </el-select>
            </el-form-item>

            <template v-if="selectedProductId">
              <el-divider>原材料配方</el-divider>
              <div v-for="(line, index) in bomLines" :key="index" style="margin-bottom: 12px;">
                <el-row :gutter="12" align="middle">
                  <el-col :span="12">
                    <el-select v-model="line.materialId" placeholder="选择原材料" filterable>
                      <el-option
                        v-for="m in rawMaterials"
                        :key="m.id"
                        :label="`${m.materialName}${m.materialSpec ? ` (${m.materialSpec})` : ''}`"
                        :value="m.id"
                      />
                    </el-select>
                  </el-col>
                  <el-col :span="6">
                    <el-input-number v-model="line.qty" :min="1" placeholder="数量" />
                  </el-col>
                  <el-col :span="4">
                    <span v-if="line.materialId">{{ getMaterialUnit(line.materialId) }}</span>
                  </el-col>
                  <el-col :span="2">
                    <el-button
                      type="danger"
                      :icon="'Delete'"
                      circle
                      size="small"
                      @click="removeBomLine(index)"
                      :disabled="bomLines.length <= 1"
                    />
                  </el-col>
                </el-row>
              </div>
              <el-button type="primary" plain @click="addBomLine">+ 添加原材料</el-button>
              <el-divider />
              <el-button type="success" :loading="bomSubmitting" @click="saveBom">
                保存 BOM 配方
              </el-button>
            </template>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.workshop-todo {
  padding: 20px;
}

.workshop-card {
  border: 0;
  border-radius: 16px;
  box-shadow: 0 14px 34px rgba(230, 81, 0, 0.15);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-size: 16px;
  font-weight: 700;
  color: #2d3340;
}

.page-subtitle {
  font-size: 12px;
  color: #8f97ab;
}

.soft-btn {
  border-radius: 10px;
}

.tab-badge :deep(.el-badge__content) {
  top: 2px;
  right: -4px;
}

.page-tabs :deep(.el-tabs__item) {
  border-radius: 10px;
  margin: 3px 0;
}

.page-tabs :deep(.el-tabs__item.is-active) {
  color: #e65100;
  background: #fff3e0;
}

.page-tabs :deep(.el-tabs__active-bar) {
  background-color: #e65100;
}

.page-tabs :deep(.el-table) {
  border-radius: 12px;
  overflow: hidden;
}

.page-tabs :deep(.el-table th.el-table__cell) {
  background: #fff3e0;
}

.page-tabs :deep(.el-button--primary:not(.is-link):not(.is-text)) {
  border: 0;
  background: linear-gradient(90deg, #ff9800 0%, #e65100 100%);
}

.page-tabs :deep(.el-button--primary:not(.is-link):not(.is-text):hover) {
  background: linear-gradient(90deg, #f57c00 0%, #bf360c 100%);
}

.page-tabs :deep(.el-button.is-link.el-button--primary) {
  color: #e65100;
}

.page-tabs :deep(.el-input__wrapper),
.page-tabs :deep(.el-textarea__inner),
.page-tabs :deep(.el-select__wrapper),
.page-tabs :deep(.el-input-number) {
  border-radius: 10px;
}
</style>
