<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as baseApi from '@/api/base'
import { materialTypeLabel } from '@/constants/admin'

const PAGE_SIZE = 5
const currentPage = ref(1)

const props = defineProps<{
  materials: baseApi.Material[]
}>()

const emit = defineEmits<{
  (e: 'refresh'): void
}>()

const pagedMaterials = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return props.materials.slice(start, start + PAGE_SIZE)
})

const showDialog = ref(false)
const submitting = ref(false)
const form = reactive({
  materialCode: '',
  materialName: '',
  materialSpec: '',
  unit: '',
  materialType: 'RAW' as 'RAW' | 'PRODUCT',
  safetyStock: 0,
})

function openDialog() {
  form.materialCode = ''
  form.materialName = ''
  form.materialSpec = ''
  form.unit = ''
  form.materialType = 'RAW'
  form.safetyStock = 0
  showDialog.value = true
}

function fillExample() {
  const timestamp = Date.now().toString().slice(-4)
  form.materialCode = `MAT-${timestamp}`
  form.materialName = `示例物料${timestamp}`
  form.materialSpec = '规格A 100mm'
  form.unit = '个'
  form.materialType = 'RAW'
  form.safetyStock = 50
}

async function onSubmit() {
  if (!form.materialCode.trim() || !form.materialName.trim()) {
    ElMessage.warning('请输入物料编码和名称')
    return
  }
  submitting.value = true
  try {
    await baseApi.createMaterial({
      materialCode: form.materialCode.trim(),
      materialName: form.materialName.trim(),
      materialSpec: form.materialSpec.trim() || undefined,
      unit: form.unit.trim() || undefined,
      materialType: form.materialType,
      safetyStock: form.safetyStock,
    })
    ElMessage.success('物料创建成功')
    showDialog.value = false
    emit('refresh')
  } catch (e: any) {
    ElMessage.error(e?.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

async function onDelete(row: baseApi.Material) {
  try {
    await ElMessageBox.confirm(`确定删除物料 "${row.materialName}" 吗？`, '警告', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
    })
    await baseApi.deleteMaterial(row.id)
    ElMessage.success('删除成功')
    emit('refresh')
  } catch {
    // ignore cancel
  }
}
</script>

<template>
  <div>
    <div class="action-bar" style="margin-bottom: 12px">
      <el-button type="primary" size="small" @click="openDialog">新增物料</el-button>
    </div>
    <el-table :data="pagedMaterials" style="width: 100%" size="small">
      <el-table-column prop="materialCode" label="物料编码" />
      <el-table-column prop="materialName" label="物料名称" />
      <el-table-column prop="materialSpec" label="规格" />
      <el-table-column prop="unit" label="单位" width="80" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          {{ materialTypeLabel(row.materialType) }}
        </template>
      </el-table-column>
      <el-table-column prop="safetyStock" label="安全库存" width="120" />
      <el-table-column prop="enabled" label="启用" width="80">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80">
        <template #default="{ row }">
          <el-button link type="danger" size="small" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="props.materials.length > PAGE_SIZE"
      v-model:current-page="currentPage"
      :page-size="PAGE_SIZE"
      :total="props.materials.length"
      layout="prev, pager, next"
      style="margin-top: 12px; justify-content: flex-end"
    />

    <el-dialog v-model="showDialog" title="新增物料" width="480px">
      <el-form label-width="90px" @submit.prevent>
        <el-form-item label="物料编码">
          <el-input v-model="form.materialCode" placeholder="如：RM-XXX" />
        </el-form-item>
        <el-form-item label="物料名称">
          <el-input v-model="form.materialName" placeholder="如：钢板" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="form.materialSpec" placeholder="可选" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="form.unit" placeholder="如：个、张、件" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.materialType">
            <el-radio value="RAW">原材料</el-radio>
            <el-radio value="PRODUCT">成品</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="安全库存">
          <el-input-number v-model="form.safetyStock" :min="0" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="fillExample">一键填入示例</el-button>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

