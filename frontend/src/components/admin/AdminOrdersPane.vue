<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as orderApi from '@/api/order'
import type * as baseApi from '@/api/base'
import { orderStatusLabel } from '@/constants/admin'

const PAGE_SIZE = 5
const currentPage = ref(1)

const props = defineProps<{
  orders: orderApi.OrderVO[]
  customers: baseApi.Customer[]
  materials: baseApi.Material[]
}>()

const emit = defineEmits<{
  (e: 'refresh'): void
}>()

const pagedOrders = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return props.orders.slice(start, start + PAGE_SIZE)
})

const customerMap = computed(() => new Map(props.customers.map(c => [c.id, c])))
const materialMap = computed(() => new Map(props.materials.map(m => [m.id, m])))

function getCustomerName(id: number | undefined) {
  if (!id) return '-'
  const c = customerMap.value.get(id)
  return c ? c.customerName : String(id)
}

function getMaterialName(id: number | undefined) {
  if (!id) return '-'
  const m = materialMap.value.get(id)
  return m ? `${m.materialCode} / ${m.materialName}` : String(id)
}

const productMaterials = computed(() => props.materials.filter(m => m.materialType === 'PRODUCT'))

const showDialog = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const submitting = ref(false)
const form = reactive({
  orderNo: '',
  customerId: undefined as number | undefined,
  productMaterialId: undefined as number | undefined,
  qty: 1,
  deliveryDate: '',
})

function genOrderNo() {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  const s = String(d.getSeconds()).padStart(2, '0')
  return `SO-${y}${m}${day}-${h}${mi}${s}`
}

function openDialog() {
  isEdit.value = false
  editingId.value = null
  form.orderNo = genOrderNo()
  form.customerId = undefined
  form.productMaterialId = undefined
  form.qty = 1
  form.deliveryDate = ''
  showDialog.value = true
}

function openEditDialog(row: orderApi.OrderVO) {
  isEdit.value = true
  editingId.value = row.id
  form.orderNo = row.orderNo
  form.customerId = row.customerId
  form.productMaterialId = row.productMaterialId
  form.qty = row.qty ?? 1
  form.deliveryDate = row.deliveryDate ?? ''
  showDialog.value = true
}

function fillExample() {
  if (!isEdit.value) {
    form.orderNo = genOrderNo()
  }
  if (props.customers.length > 0) {
    form.customerId = props.customers[0].id
  }
  if (productMaterials.value.length > 0) {
    form.productMaterialId = productMaterials.value[0].id
  }
  form.qty = 100
  const nextWeek = new Date()
  nextWeek.setDate(nextWeek.getDate() + 7)
  form.deliveryDate = nextWeek.toISOString().split('T')[0]
}

async function onSubmit() {
  if (!form.orderNo.trim()) {
    ElMessage.warning('请输入订单号')
    return
  }
  if (!form.customerId) {
    ElMessage.warning('请选择客户')
    return
  }
  if (!form.productMaterialId) {
    ElMessage.warning('请选择产品')
    return
  }
  if (form.qty <= 0) {
    ElMessage.warning('请输入有效数量')
    return
  }
  submitting.value = true
  try {
    if (isEdit.value && editingId.value) {
      await orderApi.updateOrder(editingId.value, {
        orderNo: form.orderNo.trim(),
        customerId: form.customerId,
        productMaterialId: form.productMaterialId,
        qty: form.qty,
        deliveryDate: form.deliveryDate || undefined,
      })
      ElMessage.success('订单更新成功，工单已同步更新')
    } else {
      await orderApi.createOrder({
        orderNo: form.orderNo.trim(),
        customerId: form.customerId,
        productMaterialId: form.productMaterialId,
        qty: form.qty,
        deliveryDate: form.deliveryDate || undefined,
      })
      ElMessage.success('订单创建成功，工单已自动生成')
    }
    showDialog.value = false
    emit('refresh')
  } catch (e: any) {
    ElMessage.error(e?.message || (isEdit.value ? '更新失败' : '创建失败'))
  } finally {
    submitting.value = false
  }
}

async function onDelete(row: orderApi.OrderVO) {
  try {
    await ElMessageBox.confirm(
      `确定删除订单 ${row.orderNo} 吗？\n\n注意：关联的工单也会被删除！`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await orderApi.deleteOrder(row.id)
    ElMessage.success('订单已删除')
    emit('refresh')
  } catch {
    // ignore cancel
  }
}

async function onUpdateStatus(row: orderApi.OrderVO, status: orderApi.OrderStatus) {
  try {
    await orderApi.updateOrderStatus(row.id, status)
    ElMessage.success('状态已更新')
    emit('refresh')
  } catch (e: any) {
    ElMessage.error(e?.message || '更新失败')
  }
}
</script>

<template>
  <div>
    <div class="action-bar" style="margin-bottom: 12px">
      <el-button type="primary" size="small" @click="openDialog">新增订单</el-button>
    </div>
    <el-table :data="pagedOrders" style="width: 100%" size="small">
      <el-table-column prop="orderNo" label="订单号" width="140" />
      <el-table-column label="客户" min-width="120">
        <template #default="{ row }">
          {{ getCustomerName(row.customerId) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-dropdown trigger="click" @command="(cmd: orderApi.OrderStatus) => onUpdateStatus(row, cmd)">
            <el-tag
              :type="row.status === 'DONE' ? 'success' : row.status === 'SHIPPED' ? 'primary' : row.status === 'COMPLETED' ? '' : row.status === 'PRODUCING' ? 'warning' : 'info'"
              size="small"
              style="cursor: pointer"
            >
              {{ orderStatusLabel(row.status) }} ▼
            </el-tag>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="PENDING" :disabled="row.status === 'PENDING'">待处理</el-dropdown-item>
                <el-dropdown-item command="PRODUCING" :disabled="row.status === 'PRODUCING'">生产中</el-dropdown-item>
                <el-dropdown-item command="COMPLETED" :disabled="row.status === 'COMPLETED'">已制成</el-dropdown-item>
                <el-dropdown-item command="SHIPPED" :disabled="row.status === 'SHIPPED'">已发货</el-dropdown-item>
                <el-dropdown-item command="DONE" :disabled="row.status === 'DONE'">已完成</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
      <el-table-column prop="deliveryDate" label="交货期" width="100" />
      <el-table-column label="产品" min-width="140">
        <template #default="{ row }">
          {{ getMaterialName(row.productMaterialId) }}
        </template>
      </el-table-column>
      <el-table-column prop="qty" label="数量" width="70" />
      <el-table-column prop="workOrderNo" label="工单号" width="130" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="props.orders.length > PAGE_SIZE"
      v-model:current-page="currentPage"
      :page-size="PAGE_SIZE"
      :total="props.orders.length"
      layout="prev, pager, next"
      style="margin-top: 12px; justify-content: flex-end"
    />

    <el-dialog v-model="showDialog" :title="isEdit ? '编辑订单' : '新增订单'" width="500px">
      <el-form label-width="80px" @submit.prevent>
        <el-form-item label="订单号">
          <el-input v-model="form.orderNo" placeholder="自动生成" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="客户">
          <el-select v-model="form.customerId" filterable placeholder="选择客户" style="width: 100%">
            <el-option
              v-for="c in props.customers"
              :key="c.id"
              :label="c.customerName"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="产品">
          <el-select v-model="form.productMaterialId" filterable placeholder="选择成品物料" style="width: 100%">
            <el-option
              v-for="m in productMaterials"
              :key="m.id"
              :label="`${m.materialCode} / ${m.materialName}`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="form.qty" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="交货期">
          <el-date-picker v-model="form.deliveryDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="fillExample">一键填入示例</el-button>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">{{ isEdit ? '保存' : '确定' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

