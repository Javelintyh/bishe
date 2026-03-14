<script setup lang="ts">
import type { Material } from '@/api/base'
import type { BomEditLine } from '@/types/workshop'

const props = defineProps<{
  selectedProductId: number | null
  bomLines: BomEditLine[]
  bomSubmitting: boolean
  productMaterials: Material[]
  rawMaterials: Material[]
  getMaterialUnit: (id: number) => string
}>()

const emit = defineEmits<{
  selectProduct: [productId: number]
  addLine: []
  removeLine: [index: number]
  save: []
}>()
</script>

<template>
  <el-form label-width="120px" style="max-width: 600px;">
    <el-form-item label="选择成品">
      <el-select
        :model-value="selectedProductId"
        placeholder="选择成品"
        filterable
        @update:model-value="emit('selectProduct', $event)"
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
              @click="emit('removeLine', index)"
              :disabled="bomLines.length <= 1"
            />
          </el-col>
        </el-row>
      </div>
      <el-button type="primary" plain @click="emit('addLine')">+ 添加原材料</el-button>
      <el-divider />
      <el-button type="success" :loading="bomSubmitting" @click="emit('save')">
        保存 BOM 配方
      </el-button>
    </template>
  </el-form>
</template>
