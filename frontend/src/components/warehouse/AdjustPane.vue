<script setup lang="ts">
import type { Material } from '@/api/base'
import type { AdjustForm } from '@/types/warehouse'

const props = defineProps<{
  adjustForm: AdjustForm
  materials: Material[]
  getStockQty: (materialId: number) => number
}>()

const emit = defineEmits<{
  adjust: []
}>()
</script>

<template>
  <el-form :model="adjustForm" label-width="100px" style="max-width: 400px;">
    <el-form-item label="物料">
      <el-select v-model="adjustForm.materialId" placeholder="选择物料" filterable>
        <el-option
          v-for="m in materials"
          :key="m.id"
          :label="`${m.materialName}${m.materialSpec ? ` (${m.materialSpec})` : ''} [库存: ${getStockQty(m.id)}]`"
          :value="m.id"
        />
      </el-select>
    </el-form-item>
    <el-form-item label="调整数量">
      <el-input-number v-model="adjustForm.adjustQty" />
      <span style="margin-left: 8px; color: #909399;">正数入库，负数出库</span>
    </el-form-item>
    <el-form-item label="备注">
      <el-input v-model="adjustForm.remark" type="textarea" />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="emit('adjust')">确认调整</el-button>
    </el-form-item>
  </el-form>
</template>
