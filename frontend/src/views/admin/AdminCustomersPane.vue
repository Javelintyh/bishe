<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as baseApi from '@/api/base'

const PAGE_SIZE = 5
const currentPage = ref(1)

const props = defineProps<{
  customers: baseApi.Customer[]
}>()

const emit = defineEmits<{
  (e: 'refresh'): void
}>()

const pagedCustomers = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return props.customers.slice(start, start + PAGE_SIZE)
})

const showDialog = ref(false)
const submitting = ref(false)
const form = reactive({
  customerName: '',
  contactName: '',
  contactPhone: '',
  address: '',
})

function openDialog() {
  form.customerName = ''
  form.contactName = ''
  form.contactPhone = ''
  form.address = ''
  showDialog.value = true
}

function fillExample() {
  const timestamp = Date.now().toString().slice(-4)
  form.customerName = `示例客户${timestamp}有限公司`
  form.contactName = '张经理'
  form.contactPhone = '138' + Math.floor(Math.random() * 100000000).toString().padStart(8, '0')
  form.address = '广东省东莞市松山湖科技园'
}

async function onSubmit() {
  if (!form.customerName.trim()) {
    ElMessage.warning('请输入客户名称')
    return
  }
  submitting.value = true
  try {
    await baseApi.createCustomer({
      customerName: form.customerName.trim(),
      contactName: form.contactName.trim() || undefined,
      contactPhone: form.contactPhone.trim() || undefined,
      address: form.address.trim() || undefined,
    })
    ElMessage.success('客户创建成功')
    showDialog.value = false
    emit('refresh')
  } catch (e: any) {
    ElMessage.error(e?.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

async function onDelete(row: baseApi.Customer) {
  try {
    await ElMessageBox.confirm(`确定删除客户 "${row.customerName}" 吗？`, '警告', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
    })
    await baseApi.deleteCustomer(row.id)
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
      <el-button type="primary" size="small" @click="openDialog">新增客户</el-button>
    </div>
    <el-table :data="pagedCustomers" style="width: 100%" size="small">
      <el-table-column prop="customerName" label="客户名称" />
      <el-table-column prop="contactName" label="联系人" />
      <el-table-column prop="contactPhone" label="电话" />
      <el-table-column prop="address" label="地址" />
      <el-table-column label="操作" width="80">
        <template #default="{ row }">
          <el-button link type="danger" size="small" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="props.customers.length > PAGE_SIZE"
      v-model:current-page="currentPage"
      :page-size="PAGE_SIZE"
      :total="props.customers.length"
      layout="prev, pager, next"
      style="margin-top: 12px; justify-content: flex-end"
    />

    <el-dialog v-model="showDialog" title="新增客户" width="480px">
      <el-form label-width="80px" @submit.prevent>
        <el-form-item label="客户名称">
          <el-input v-model="form.customerName" placeholder="如：XX有限公司" />
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

