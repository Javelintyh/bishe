<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as baseApi from '@/api/base'
import type { Material } from '@/api/base'

const PAGE_SIZE = 5
const currentPage = ref(1)

const props = defineProps<{
  suppliers: baseApi.Supplier[]
}>()

const emit = defineEmits<{
  (e: 'refresh'): void
}>()

const pagedSuppliers = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return props.suppliers.slice(start, start + PAGE_SIZE)
})

const showDialog = ref(false)
const submitting = ref(false)
const form = reactive({
  supplierName: '',
  contactName: '',
  contactPhone: '',
  address: '',
  rawMaterialIds: [] as number[],
})

function openDialog() {
  form.supplierName = ''
  form.contactName = ''
  form.contactPhone = ''
  form.address = ''
  form.rawMaterialIds = []
  showDialog.value = true
}

function fillExample() {
  const timestamp = Date.now().toString().slice(-4)
  form.supplierName = `示例供应商${timestamp}有限公司`
  form.contactName = '李经理'
  form.contactPhone = '139' + Math.floor(Math.random() * 100000000).toString().padStart(8, '0')
  form.address = '广东省东莞市厚街镇工业区'
  if (rawMaterials.value.length) {
    form.rawMaterialIds = rawMaterials.value.slice(0, 3).map((m) => m.id)
  }
}

async function onSubmit() {
  if (!form.supplierName.trim()) {
    ElMessage.warning('请输入供应商名称')
    return
  }
  if (!form.rawMaterialIds.length) {
    ElMessage.warning('请至少选择一个可售原材料')
    return
  }
  submitting.value = true
  try {
    await baseApi.createSupplier({
      supplierName: form.supplierName.trim(),
      contactName: form.contactName.trim() || undefined,
      contactPhone: form.contactPhone.trim() || undefined,
      address: form.address.trim() || undefined,
      rawMaterialIds: form.rawMaterialIds.length ? form.rawMaterialIds : undefined,
    })
    ElMessage.success('供应商创建成功')
    showDialog.value = false
    emit('refresh')
  } catch (e: any) {
    ElMessage.error(e?.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

async function onDelete(row: baseApi.Supplier) {
  try {
    await ElMessageBox.confirm(`确定删除供应商 "${row.supplierName}" 吗？`, '警告', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
    })
    await baseApi.deleteSupplier(row.id)
    ElMessage.success('删除成功')
    emit('refresh')
  } catch {
    // ignore cancel
  }
}

const rawMaterials = ref<Material[]>([])
async function loadRawMaterials() {
  try {
    const resp = await baseApi.listMaterials({ materialType: 'RAW' })
    rawMaterials.value = resp.data ?? []
  } catch {
    rawMaterials.value = []
  }
}

onMounted(loadRawMaterials)

const supplierRawMaterials = ref<Record<number, Material[]>>({})
const supplierRawMaterialsLoading = ref<Record<number, boolean>>({})
const supplierRawMaterialsFetched = ref<Record<number, boolean>>({})

async function ensureSupplierRawMaterials(supplierId: number) {
  if (supplierRawMaterialsFetched.value[supplierId]) return
  if (supplierRawMaterialsLoading.value[supplierId]) return

  supplierRawMaterialsLoading.value[supplierId] = true
  try {
    const resp = await baseApi.listRawMaterialsBySupplier(supplierId)
    if (resp.code === 0) {
      supplierRawMaterials.value[supplierId] = resp.data ?? []
      supplierRawMaterialsFetched.value[supplierId] = true
    }
  } catch {
    supplierRawMaterials.value[supplierId] = []
    supplierRawMaterialsFetched.value[supplierId] = true
  } finally {
    supplierRawMaterialsLoading.value[supplierId] = false
  }
}

watch(
  () => pagedSuppliers.value.map((s) => s.id).join(','),
  async () => {
    const ids = pagedSuppliers.value.map((s) => s.id)
    await Promise.all(ids.map((id) => ensureSupplierRawMaterials(id)))
  },
  { immediate: true },
)

function formatMaterial(m: Material) {
  return `${m.materialCode} ${m.materialName}`
}

function getSupplierMaterialsSummary(supplierId: number) {
  const list = supplierRawMaterials.value[supplierId]
  if (!list) {
    return supplierRawMaterialsLoading.value[supplierId] ? '加载中...' : '-'
  }
  if (!list.length) return '无'
  const first = list.slice(0, 3).map((m) => m.materialCode).join('，')
  return list.length > 3 ? `${first}...` : first
}
</script>

<template>
  <div>
    <div class="action-bar" style="margin-bottom: 12px">
      <el-button type="primary" size="small" @click="openDialog">新增供应商</el-button>
    </div>
    <el-table :data="pagedSuppliers" style="width: 100%" size="small">
      <el-table-column prop="supplierName" label="供应商名称" />
      <el-table-column prop="contactName" label="联系人" />
      <el-table-column prop="contactPhone" label="电话" />
      <el-table-column prop="address" label="地址" />
      <el-table-column label="可售原材料" min-width="240">
        <template #default="{ row }">
          <el-popover
            placement="top"
            trigger="hover"
            :width="420"
          >
            <template #default>
              <div style="max-height: 180px; overflow: auto">
                <div v-if="supplierRawMaterials[row.id]?.length">
                  <div
                    v-for="m in supplierRawMaterials[row.id]"
                    :key="m.id"
                    style="margin-bottom: 4px"
                  >
                    {{ formatMaterial(m) }}
                  </div>
                </div>
                <div v-else>
                  {{
                    supplierRawMaterialsLoading[row.id]
                      ? '加载中...'
                      : '无'
                  }}
                </div>
              </div>
            </template>
            <template #reference>
              <span>{{ getSupplierMaterialsSummary(row.id) }}</span>
            </template>
          </el-popover>
        </template>
      </el-table-column>
      <el-table-column label="启用" width="80">
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
      v-if="props.suppliers.length > PAGE_SIZE"
      v-model:current-page="currentPage"
      :page-size="PAGE_SIZE"
      :total="props.suppliers.length"
      layout="prev, pager, next"
      style="margin-top: 12px; justify-content: flex-end"
    />

    <el-dialog v-model="showDialog" title="新增供应商" width="480px">
      <el-form label-width="90px" @submit.prevent>
        <el-form-item label="供应商名称">
          <el-input v-model="form.supplierName" placeholder="如：XX供应商有限公司" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactName" placeholder="可选" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" placeholder="可选" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" placeholder="可选" />
        </el-form-item>
        <el-form-item label="可售原材料">
          <el-select
            v-model="form.rawMaterialIds"
            multiple
            filterable
            clearable
            placeholder="选择原材料（可多选）"
            style="width: 100%"
          >
            <el-option
              v-for="m in rawMaterials"
              :key="m.id"
              :label="formatMaterial(m)"
              :value="m.id"
            />
          </el-select>
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

