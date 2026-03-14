<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as baseApi from '@/api/base'

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
})

function openDialog() {
  form.supplierName = ''
  form.contactName = ''
  form.contactPhone = ''
  form.address = ''
  showDialog.value = true
}

function fillExample() {
  const timestamp = Date.now().toString().slice(-4)
  form.supplierName = `示例供应商${timestamp}有限公司`
  form.contactName = '李经理'
  form.contactPhone = '139' + Math.floor(Math.random() * 100000000).toString().padStart(8, '0')
  form.address = '广东省东莞市厚街镇工业区'
}

async function onSubmit() {
  if (!form.supplierName.trim()) {
    ElMessage.warning('请输入供应商名称')
    return
  }
  submitting.value = true
  try {
    await baseApi.createSupplier({
      supplierName: form.supplierName.trim(),
      contactName: form.contactName.trim() || undefined,
      contactPhone: form.contactPhone.trim() || undefined,
      address: form.address.trim() || undefined,
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
      </el-form>
      <template #footer>
        <el-button size="small" @click="fillExample">一键填入示例</el-button>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

