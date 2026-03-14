<script setup lang="ts">
import type { Material } from '@/api/base'
import type { ProductionWorkOrder } from '@/api/production'
import type { InboundForm, SalesOutboundForm } from '@/types/warehouse'

const props = defineProps<{
  inboundForm: InboundForm
  salesOutboundForm: SalesOutboundForm
  productMaterials: Material[]
  pendingInboundWorkOrders: ProductionWorkOrder[]
  getStockQty: (materialId: number) => number
  getMaterialInfo: (materialId: number) => string
}>()

const emit = defineEmits<{
  selectInboundWorkOrder: [workOrderNo: string]
  inbound: []
  salesOutbound: []
}>()
</script>

<template>
  <el-row :gutter="24">
    <el-col :span="12">
      <h4 style="color: #67c23a; margin-bottom: 16px;">成品入库</h4>
      <el-form :model="inboundForm" label-width="100px">
        <el-form-item label="关联工单">
          <el-select
            v-model="inboundForm.workOrderNo"
            placeholder="可选择工单"
            clearable
            filterable
            @change="emit('selectInboundWorkOrder', $event)"
          >
            <el-option
              v-for="wo in pendingInboundWorkOrders"
              :key="wo.workOrderNo"
              :label="`${wo.workOrderNo} - ${getMaterialInfo(wo.productMaterialId)}`"
              :value="wo.workOrderNo"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="成品">
          <el-select v-model="inboundForm.materialId" placeholder="选择成品" filterable>
            <el-option
              v-for="m in productMaterials"
              :key="m.id"
              :label="`${m.materialName}${m.materialSpec ? ` (${m.materialSpec})` : ''}`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="入库数量">
          <el-input-number v-model="inboundForm.qty" :min="1" />
        </el-form-item>
        <el-form-item>
          <el-button type="success" @click="emit('inbound')">确认入库</el-button>
        </el-form-item>
      </el-form>
    </el-col>
    <el-col :span="12">
      <h4 style="color: #f56c6c; margin-bottom: 16px;">成品出库</h4>
      <el-form :model="salesOutboundForm" label-width="100px">
        <el-form-item label="成品">
          <el-select v-model="salesOutboundForm.materialId" placeholder="选择成品" filterable>
            <el-option
              v-for="m in productMaterials"
              :key="m.id"
              :label="`${m.materialName}${m.materialSpec ? ` (${m.materialSpec})` : ''} [库存: ${getStockQty(m.id)}]`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="出库数量">
          <el-input-number v-model="salesOutboundForm.qty" :min="1" />
        </el-form-item>
        <el-form-item label="销售单号">
          <el-input v-model="salesOutboundForm.bizId" placeholder="可选" />
        </el-form-item>
        <el-form-item>
          <el-button type="danger" @click="emit('salesOutbound')">确认出库</el-button>
        </el-form-item>
      </el-form>
    </el-col>
  </el-row>
</template>
