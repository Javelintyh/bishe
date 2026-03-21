<script setup lang="ts">
/**
 * 车间端看板主页面
 *
 * 功能模块：
 * - 生产看板（工单状态分布图）
 * - 待生产工单列表
 * - 生产报工
 * - 报工记录
 * - BOM 配方管理
 */
import { onMounted, ref } from 'vue'
import { useWorkshopData, useReportForm, useBomEditor, useWorkshopMessages, useWorkshopPageActions } from '@/composables/workshop'
import {
  WorkOrderTable,
  ReportFormPane,
  ReportHistoryTable,
  BomEditorPane,
  KanbanChart,
  WorkshopMessagesPane,
} from '@/components/workshop'
import type { WorkshopTabName } from '@/types/workshop'

const activeTab = ref<WorkshopTabName>('kanban')
const {
  workshopMessages,
  messagesLoading,
  workshopMessageCount,
  loadWorkshopMessages,
  handleWorkshopMessage,
} = useWorkshopMessages()

const {
  loading,
  state,
  loadData,
  productMaterials,
  rawMaterials,
  reportableWorkOrders,
  getMaterialName,
  getMaterialUnit,
  getProductBomInfo,
} = useWorkshopData()

const { refreshAll, initPage } = useWorkshopPageActions({
  loadData,
  loadWorkshopMessages,
})

const {
  reportForm,
  onSelectReportWorkOrder,
  goToReport,
  onReport,
} = useReportForm({
  loadData,
  getWorkOrders: () => state.workOrders,
  getMaterialName,
  activeTab,
})

const {
  selectedProductId,
  bomLines,
  bomSubmitting,
  onSelectProduct,
  addBomLine,
  removeBomLine,
  saveBom,
} = useBomEditor({
  loadData,
  getBomMap: () => state.bomMap,
})

onMounted(initPage)
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
          <el-button
            class="soft-btn"
            type="primary"
            :loading="loading || messagesLoading"
            @click="refreshAll"
          >
            刷新数据
          </el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab" tab-position="left" class="page-tabs" style="min-height: 420px">
        <!-- 生产看板 -->
        <el-tab-pane label="生产看板" name="kanban">
          <KanbanChart :all-work-orders="state.allWorkOrders" />
        </el-tab-pane>

        <!-- 待生产工单 -->
        <el-tab-pane name="pending">
          <template #label>
            <el-badge :value="reportableWorkOrders.length" :hidden="reportableWorkOrders.length === 0" :max="99" class="tab-badge">
              <span>待生产工单</span>
            </el-badge>
          </template>
          <WorkOrderTable
            :work-orders="reportableWorkOrders"
            :loading="loading"
            :get-material-name="getMaterialName"
            :get-product-bom-info="getProductBomInfo"
            @go-to-report="goToReport"
          />
        </el-tab-pane>

        <!-- 生产报工 -->
        <el-tab-pane label="生产报工" name="report">
          <ReportFormPane
            :report-form="reportForm"
            :reportable-work-orders="reportableWorkOrders"
            :reasons="state.reasons"
            :get-material-name="getMaterialName"
            @select-work-order="onSelectReportWorkOrder"
            @report="onReport"
          />
        </el-tab-pane>

        <!-- 报工记录 -->
        <el-tab-pane label="报工记录" name="history">
          <ReportHistoryTable :reports="state.reports" :loading="loading" />
        </el-tab-pane>

        <!-- BOM 配方 -->
        <el-tab-pane label="BOM 配方" name="bom">
          <BomEditorPane
            :selected-product-id="selectedProductId"
            :bom-lines="bomLines"
            :bom-submitting="bomSubmitting"
            :product-materials="productMaterials"
            :raw-materials="rawMaterials"
            :get-material-unit="getMaterialUnit"
            @select-product="onSelectProduct"
            @add-line="addBomLine"
            @remove-line="removeBomLine"
            @save="saveBom"
          />
        </el-tab-pane>

        <el-tab-pane name="messages">
          <template #label>
            <el-badge :value="workshopMessageCount" :hidden="workshopMessageCount === 0" :max="99" class="tab-badge">
              <span>消息</span>
            </el-badge>
          </template>
          <WorkshopMessagesPane
            :messages="workshopMessages"
            :loading="messagesLoading"
            :on-acknowledge="handleWorkshopMessage"
          />
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
