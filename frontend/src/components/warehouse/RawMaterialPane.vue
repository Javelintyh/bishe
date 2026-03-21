<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import type { Material } from '@/api/base'
import type { ProductionWorkOrder } from '@/api/production'
import type { PurchaseInboundForm, OutboundForm } from '@/types/warehouse'
import * as purchaseApi from '@/api/purchase'

const props = defineProps<{
  purchaseInboundForm: PurchaseInboundForm
  outboundForm: OutboundForm
  rawMaterials: Material[]
  pendingOutboundWorkOrders: ProductionWorkOrder[]
  getStockQty: (materialId: number) => number
  getMaterialInfo: (materialId: number) => string
  purchaseButtonLabel?: string
}>()

const emit = defineEmits<{
  selectOutboundWorkOrder: [workOrderNo: string]
  purchaseInbound: []
  outbound: []
}>()

const purchaseOrders = ref<{ id: number; poNo: string }[]>([])

async function loadPurchaseOrders() {
  const resp = await purchaseApi.listPurchaseOrders('RECEIVING')
  const poList = resp.data ?? []
  purchaseOrders.value = poList.map((po) => ({ id: po.id, poNo: po.poNo }))
}

const autofillToken = ref(0)
watch(
  () => props.purchaseInboundForm.bizId,
  async (bizId) => {
    const token = ++autofillToken.value
    const poId = bizId ? Number(bizId) : NaN
    if (!Number.isFinite(poId) || poId <= 0) {
      props.purchaseInboundForm.materialId = null
      props.purchaseInboundForm.qty = 1
      return
    }

    const resp = await purchaseApi.getPurchaseOrderDetails(poId)
    if (token !== autofillToken.value) return
    if (resp.code !== 0) return

    const details = resp.data ?? []
    if (!details.length) return

    // 优先填充“未入库完”的第一条明细
    const firstPending = details.find((d) => Number(d.qty) - Number(d.receivedQty ?? 0) > 0)
    const target = firstPending ?? details[0]
    if (!target) return
    const remainQty = Math.max(1, Math.ceil(Number(target.qty) - Number(target.receivedQty ?? 0)))

    props.purchaseInboundForm.materialId = target.materialId
    props.purchaseInboundForm.qty = remainQty
  },
  { immediate: true },
)

onMounted(loadPurchaseOrders)
</script>

<template>
  <el-row :gutter="24">
    <el-col :span="12">
      <h4 style="color: #67c23a; margin-bottom: 16px;">原材料入库（采购到货）</h4>
      <el-form :model="purchaseInboundForm" label-width="100px">
        <el-form-item label="采购订单">
          <el-select
            v-model="purchaseInboundForm.bizId"
            placeholder="可选：选择采购订单"
            clearable
            filterable
          >
            <el-option
              v-for="p in purchaseOrders"
              :key="p.id"
              :label="p.poNo"
              :value="String(p.id)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="原材料">
          <el-select v-model="purchaseInboundForm.materialId" placeholder="选择原材料" filterable>
            <el-option
              v-for="m in rawMaterials"
              :key="m.id"
              :label="`${m.materialName}${m.materialSpec ? ` (${m.materialSpec})` : ''}`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="入库数量">
          <el-input-number v-model="purchaseInboundForm.qty" :min="1" :max="999" />
        </el-form-item>
        <el-form-item>
          <el-button type="success" @click="emit('purchaseInbound')">
            {{ props.purchaseButtonLabel || '确认入库' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-col>
    <el-col :span="12">
      <h4 style="color: #f56c6c; margin-bottom: 16px;">原材料出库（生产领料）</h4>
      <el-form :model="outboundForm" label-width="100px">
        <el-form-item label="关联工单">
          <el-select
            v-model="outboundForm.workOrderNo"
            placeholder="可选择工单"
            clearable
            filterable
            @change="emit('selectOutboundWorkOrder', $event)"
          >
            <el-option
              v-for="wo in pendingOutboundWorkOrders"
              :key="wo.workOrderNo"
              :label="`${wo.workOrderNo} - ${getMaterialInfo(wo.productMaterialId)}`"
              :value="wo.workOrderNo"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="原材料">
          <el-select v-model="outboundForm.materialId" placeholder="选择原材料" filterable>
            <el-option
              v-for="m in rawMaterials"
              :key="m.id"
              :label="`${m.materialName}${m.materialSpec ? ` (${m.materialSpec})` : ''} [库存: ${getStockQty(m.id)}]`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="出库数量">
          <el-input-number v-model="outboundForm.qty" :min="1" />
        </el-form-item>
        <el-form-item>
          <el-button type="danger" @click="emit('outbound')">确认出库</el-button>
        </el-form-item>
      </el-form>
    </el-col>
  </el-row>
</template>
