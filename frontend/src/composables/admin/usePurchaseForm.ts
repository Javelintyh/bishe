/**
 * 采购单表单组合式函数
 */
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as purchaseApi from '@/api/purchase'
import type { PurchaseForm, PurchaseFormLineInput } from '@/types/admin'
import { genPurchaseNo } from '@/utils/format'

interface UsePurchaseFormOptions {
  loadPurchaseOrders: () => Promise<void>
}

/**
 * Notes:
 * - 采购单表单逻辑封装
 *
 * Args:
 * - options: 依赖项
 *
 * Returns:
 * - showPurchaseDialog: 对话框显示状态
 * - purchaseForm: 采购单表单
 * - purchaseFormLine: 明细行输入
 * - purchaseSubmitting: 提交状态
 * - openPurchaseDialog: 打开对话框
 * - addPurchaseLine: 添加明细行
 * - removePurchaseLine: 删除明细行
 * - submitPurchaseOrder: 提交采购单
 */
export function usePurchaseForm(options: UsePurchaseFormOptions) {
  const { loadPurchaseOrders } = options

  const showPurchaseDialog = ref(false)
  const purchaseSubmitting = ref(false)

  const purchaseForm = reactive<PurchaseForm>({
    poNo: '',
    supplierId: undefined,
    expectedDate: '',
    remark: '',
    lines: [],
  })

  const purchaseFormLine = reactive<PurchaseFormLineInput>({
    materialId: undefined,
    qty: 0,
    price: undefined,
    remark: '',
  })

  function resetPurchaseForm() {
    purchaseForm.poNo = ''
    purchaseForm.supplierId = undefined
    purchaseForm.expectedDate = ''
    purchaseForm.remark = ''
    purchaseForm.lines = []
  }

  function resetPurchaseFormLine() {
    purchaseFormLine.materialId = undefined
    purchaseFormLine.qty = 0
    purchaseFormLine.price = undefined
    purchaseFormLine.remark = ''
  }

  function openPurchaseDialog() {
    purchaseForm.poNo = genPurchaseNo()
    purchaseForm.supplierId = undefined
    purchaseForm.expectedDate = ''
    purchaseForm.remark = ''
    purchaseForm.lines = []
    resetPurchaseFormLine()
    showPurchaseDialog.value = true
  }

  function addPurchaseLine() {
    if (purchaseFormLine.materialId == null || purchaseFormLine.qty <= 0) {
      ElMessage.warning('请选择物料并输入数量')
      return
    }
    purchaseForm.lines.push({
      materialId: purchaseFormLine.materialId,
      qty: purchaseFormLine.qty,
      price: purchaseFormLine.price,
      remark: purchaseFormLine.remark || undefined,
    })
    resetPurchaseFormLine()
  }

  function removePurchaseLine(idx: number) {
    purchaseForm.lines.splice(idx, 1)
  }

  async function submitPurchaseOrder() {
    if (!purchaseForm.poNo.trim()) {
      ElMessage.warning('请输入采购单号')
      return
    }
    if (purchaseForm.lines.length === 0) {
      ElMessage.warning('请至少添加一行物料')
      return
    }
    purchaseSubmitting.value = true
    try {
      await purchaseApi.createPurchaseOrder({
        poNo: purchaseForm.poNo.trim(),
        supplierId: purchaseForm.supplierId,
        expectedDate: purchaseForm.expectedDate || undefined,
        remark: purchaseForm.remark || undefined,
        lines: purchaseForm.lines.map((l) => ({
          materialId: l.materialId,
          qty: l.qty,
          price: l.price,
          remark: l.remark,
        })),
      })
      ElMessage.success('创建成功')
      showPurchaseDialog.value = false
      await loadPurchaseOrders()
    } catch (e: any) {
      ElMessage.error(e?.message || '创建失败')
    } finally {
      purchaseSubmitting.value = false
    }
  }

  return {
    showPurchaseDialog,
    purchaseForm,
    purchaseFormLine,
    purchaseSubmitting,
    openPurchaseDialog,
    addPurchaseLine,
    removePurchaseLine,
    submitPurchaseOrder,
  }
}
