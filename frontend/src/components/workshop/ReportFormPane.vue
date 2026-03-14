<script setup lang="ts">
import type { ProductionWorkOrder } from '@/api/production'
import type { ReportForm, BadReason } from '@/types/workshop'

const props = defineProps<{
  reportForm: ReportForm
  reportableWorkOrders: ProductionWorkOrder[]
  reasons: BadReason[]
  getMaterialName: (id: number) => string
}>()

const emit = defineEmits<{
  selectWorkOrder: [workOrderNo: string]
  report: []
}>()
</script>

<template>
  <el-form :model="reportForm" label-width="120px" style="max-width: 500px;">
    <el-form-item label="工单号">
      <el-select
        v-model="reportForm.workOrderNo"
        placeholder="选择工单"
        filterable
        @change="emit('selectWorkOrder', $event)"
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
          v-for="r in reasons"
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
      <el-button type="primary" @click="emit('report')">提交报工</el-button>
    </el-form-item>
  </el-form>
</template>
