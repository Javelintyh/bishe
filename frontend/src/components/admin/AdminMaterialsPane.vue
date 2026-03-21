<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as baseApi from '@/api/base'
import { materialTypeLabel } from '@/constants/admin'

const PAGE_SIZE = 5
const currentPage = ref(1)
const typeFilter = ref<'ALL' | 'RAW' | 'PRODUCT'>('ALL')
const keyword = ref('')

const props = defineProps<{
  materials: baseApi.Material[]
}>()

const emit = defineEmits<{
  (e: 'refresh'): void
}>()

const filteredMaterials = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()
  return props.materials.filter((m) => {
    const typeMatched = typeFilter.value === 'ALL' || m.materialType === typeFilter.value
    if (!typeMatched) return false
    if (!normalizedKeyword) return true

    const haystack = [
      m.materialCode,
      m.materialName,
      m.materialSpec ?? '',
      m.unit ?? '',
    ]
      .join(' ')
      .toLowerCase()
    return haystack.includes(normalizedKeyword)
  })
})

watch([typeFilter, keyword], () => {
  currentPage.value = 1
})

const pagedMaterials = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return filteredMaterials.value.slice(start, start + PAGE_SIZE)
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

// 行内安全库存编辑：点击数字进入编辑，失焦保存
const editingSafetyStockId = ref<number | null>(null)
const safetyStockDraft = ref<number>(0)
const safetyStockSaving = ref(false)

function startEditSafetyStock(row: baseApi.Material) {
  editingSafetyStockId.value = row.id
  safetyStockDraft.value = Number(row.safetyStock ?? 0)
}

async function saveSafetyStock(row: baseApi.Material) {
  if (editingSafetyStockId.value !== row.id) return
  if (safetyStockSaving.value) return

  safetyStockSaving.value = true
  try {
    await baseApi.updateMaterial(row.id, {
      materialName: row.materialName,
      materialSpec: row.materialSpec ?? undefined,
      unit: row.unit ?? undefined,
      materialType: row.materialType,
      safetyStock: safetyStockDraft.value,
      enabled: row.enabled,
    })
    ElMessage.success('安全库存已更新')
    editingSafetyStockId.value = null
    emit('refresh')
  } catch (e: any) {
    ElMessage.error(e?.message || '更新失败')
  } finally {
    safetyStockSaving.value = false
  }
}

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
      <el-input
        v-model="keyword"
        size="small"
        clearable
        placeholder="搜索编码/名称/规格/单位"
        style="width: 260px; margin-left: 8px"
      />
    </div>
    <el-table :data="pagedMaterials" style="width: 100%" size="small">
      <el-table-column prop="materialCode" label="物料编码" />
      <el-table-column prop="materialName" label="物料名称" />
      <el-table-column prop="materialSpec" label="规格" />
      <el-table-column prop="unit" label="单位" width="80" />
      <el-table-column label="类型" width="140">
        <template #header>
          <div class="type-header">
            <span>类型</span>
            <el-select
              v-model="typeFilter"
              size="small"
              class="type-select"
            >
              <el-option label="全部" value="ALL" />
              <el-option label="原材料" value="RAW" />
              <el-option label="成品" value="PRODUCT" />
            </el-select>
          </div>
        </template>
        <template #default="{ row }">
          {{ materialTypeLabel(row.materialType) }}
        </template>
      </el-table-column>
      <el-table-column label="安全库存" width="120">
        <template #default="{ row }">
          <span
            v-if="editingSafetyStockId !== row.id"
            class="safety-stock-value"
            @click="startEditSafetyStock(row)"
          >
            {{ row.safetyStock }}
          </span>
          <el-input-number
            v-else
            v-model="safetyStockDraft"
            :min="0"
            style="width: 100%"
            size="small"
            :controls="false"
            :disabled="safetyStockSaving"
            @change="saveSafetyStock(row)"
            @blur="saveSafetyStock(row)"
          />
        </template>
      </el-table-column>
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
      v-if="filteredMaterials.length > PAGE_SIZE"
      v-model:current-page="currentPage"
      :page-size="PAGE_SIZE"
      :total="filteredMaterials.length"
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

<style scoped>
.safety-stock-value {
  cursor: pointer;
  color: #409eff;
  font-weight: 500;
}

.type-header {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap
}

.type-select {
  width: 100px;
}

/* 美化下拉框 */
.type-select :deep(.el-select__wrapper) {
  background-color: #f9f9f9;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
  transition: all 0.3s ease;
  padding: 4px 12px;
  min-height: 32px;
}

.type-select :deep(.el-select__wrapper:hover) {
  background-color: #ffffff;
  border-color: #c0c4cc;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.type-select :deep(.el-select__wrapper.is-focused) {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64,158,255,0.2);
}

/* 美化下拉箭头 */
.type-select :deep(.el-select__caret) {
  color: #909399;
  font-size: 14px;
  transition: transform 0.3s;
}

.type-select :deep(.el-select__caret.is-reverse) {
  transform: rotate(180deg);
}

/* 下拉菜单样式 */
.type-select :deep(.el-select-dropdown) {
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  margin-top: 4px;
}

.type-select :deep(.el-select-dropdown__item) {
  border-radius: 4px;
  margin: 4px 8px;
  padding: 8px 12px;
  height: auto;
  line-height: 1.4;
}

.type-select :deep(.el-select-dropdown__item.selected) {
  background-color: #ecf5ff;
  color: #409eff;
  font-weight: 500;
}

.type-select :deep(.el-select-dropdown__item.hover) {
  background-color: #f5f7fa;
}
</style>

