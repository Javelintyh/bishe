import { computed, nextTick, ref, watch, type Ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as messageApi from '@/api/message'
import * as baseApi from '@/api/base'
import * as orderApi from '@/api/order'
import * as purchaseApi from '@/api/purchase'
import type { MessageNotice } from '@/api/message'
import type { AdminTabName, PurchaseForm } from '@/types/admin'

type UseAdminDashboardActionsOptions = {
  activeTab: Ref<AdminTabName>
  state: {
    workOrders: Array<{ status: string; workOrderNo?: string; productMaterialId?: number }>
    messages: MessageNotice[]
    materials: Array<{ id: number; materialCode: string; materialType: string }>
    customers: Array<{ id: number }>
    purchaseOrders: Array<{ id: number; status: string }>
  }
  loadMessages: () => Promise<void>
  onOrderRefresh: () => Promise<void>
  onScanMessages: () => Promise<void>
  openPurchaseDialog: () => void
  showPurchaseDialog: Ref<boolean>
  purchaseForm: PurchaseForm
  submitPurchaseOrder: () => Promise<boolean>
}

export function useAdminDashboardActions(options: UseAdminDashboardActionsOptions) {
  const {
    activeTab,
    state,
    loadMessages,
    onOrderRefresh,
    onScanMessages,
    openPurchaseDialog,
    showPurchaseDialog,
    purchaseForm,
    submitPurchaseOrder,
  } = options

  const pendingStockLowPurchaseMsgId = ref<number | null>(null)
  const messagesReminderCount = computed(() => state.messages.length)
  const shownCapacityWarningIds = new Set<number>()

  async function onWorkOrderRemind(msg: MessageNotice) {
    try {
      await messageApi.markRead(msg.id)
    } catch {
      // ignore
    }
    await onScanMessages()
  }

  const pendingWorkOrderNosSignature = computed(() =>
    state.workOrders
      .filter((w) => w.status === 'TO_PRODUCE' && w.workOrderNo)
      .map((w) => w.workOrderNo as string)
      .sort()
      .join(','),
  )

  const messagesSignature = computed(() =>
    state.messages
      .map((m) => `${m.noticeType}|${m.relatedType ?? ''}|${m.relatedId ?? ''}|${m.id}`)
      .sort()
      .join('|'),
  )

  async function cleanupFinishedWorkOrderMessages() {
    const activeNos = new Set(
      state.workOrders
        .filter((w) => w.status === 'TO_PRODUCE' && w.workOrderNo)
        .map((w) => w.workOrderNo as string),
    )

    const removeIds = new Set(
      state.messages
        .filter(
          (m) =>
            (m.noticeType === 'WO_DUE_SOON' || m.noticeType === 'WO_OVERDUE') &&
            m.relatedType === 'WORK_ORDER' &&
            m.relatedId &&
            !activeNos.has(m.relatedId),
        )
        .map((m) => m.id),
    )
    if (!removeIds.size) return
    try {
      await Promise.all(Array.from(removeIds).map((id) => messageApi.markRead(id)))
    } catch {
      // ignore
    }
  }

  let cleanupRunning = false
  watch([pendingWorkOrderNosSignature, messagesSignature], async () => {
    if (cleanupRunning) return
    cleanupRunning = true
    try {
      await cleanupFinishedWorkOrderMessages()
    } finally {
      cleanupRunning = false
    }
  }, { immediate: true })

  function closePurchaseDialog() {
    showPurchaseDialog.value = false
    pendingStockLowPurchaseMsgId.value = null
  }

  async function onAfterCreateOrderByMessage(messageId: number) {
    try {
      await messageApi.markRead(messageId)
      await loadMessages()
    } catch (e: any) {
      ElMessage.error(e?.message || '消息置已读失败')
    }
  }

  async function submitPurchaseOrderFromStockLowMsg(): Promise<boolean> {
    const ok = await submitPurchaseOrder()
    if (!ok) return ok
    pendingStockLowPurchaseMsgId.value = null
    return ok
  }

  async function hasReceivingPurchasePending(materialId: number): Promise<boolean> {
    const receivingOrders = state.purchaseOrders.filter((po) => po.status === 'RECEIVING')
    if (!receivingOrders.length) return false
    for (const po of receivingOrders) {
      try {
        const detailResp = await purchaseApi.getPurchaseOrderDetails(po.id)
        const details = detailResp.data ?? []
        const hit = details.some((d) => d.materialId === materialId && Number(d.qty) > Number(d.receivedQty ?? 0))
        if (hit) return true
      } catch {
        // ignore
      }
    }
    return false
  }

  async function onCreatePurchaseFromRequest(msg: MessageNotice) {
    activeTab.value = 'purchase'
    await nextTick()
    openPurchaseDialog()
    const materialCode = msg.relatedId
    const material = state.materials.find((m) => m.materialCode === materialCode)
    if (material) {
      let suggestedQty = 0
      const lines = (msg.content || '').split('\n')
      const qtyLine = lines.find((l) => l.includes('建议采购数量'))
      if (qtyLine) {
        const numStr = qtyLine.replace(/[^\d.]/g, '')
        const val = Number(numStr)
        if (Number.isFinite(val) && val > 0) suggestedQty = val
      }
      if (!suggestedQty) suggestedQty = 1
      const remarkLine = lines.find((l) => l.includes('备注:'))
      const remark = remarkLine ? remarkLine.split('备注:')[1]?.trim() : ''
      if (remark) purchaseForm.remark = remark

      try {
        const supplierResp = await baseApi.listSuppliersByRawMaterial(material.id)
        if (supplierResp.code === 0) {
          const firstSupplier = supplierResp.data?.[0]
          if (firstSupplier?.id != null) purchaseForm.supplierId = firstSupplier.id
        }
      } catch {
        // ignore
      }

      purchaseForm.lines = [
        {
          materialId: material.id,
          qty: suggestedQty,
          price: undefined,
          remark: remark || undefined,
        },
      ]
    }
    try {
      await messageApi.markRead(msg.id)
      await loadMessages()
    } catch {
      // ignore
    }
  }

  async function onCreatePurchaseFromStockLow(msg: MessageNotice) {
    pendingStockLowPurchaseMsgId.value = null
    activeTab.value = 'purchase'
    await nextTick()
    const contentLines = (msg.content || '').split('\n').map((l) => l.trim())
    const materialCode =
      msg.relatedId ||
      contentLines.find((l) => l.startsWith('物料编码:'))?.split('物料编码:')[1]?.trim()
    if (!materialCode) {
      ElMessage.error('库存预警消息缺少物料编码')
      return
    }
    const material = state.materials.find((m) => m.materialCode === materialCode)
    if (!material) {
      ElMessage.error('未找到对应物料')
      return
    }
    const parseNumberFromLine = (line: string | undefined) => {
      if (!line) return NaN
      const m = line.match(/-?\d+(\.\d+)?/)
      if (!m) return NaN
      return Number(m[0])
    }
    const currentStock = parseNumberFromLine(contentLines.find((l) => l.includes('当前库存:')))
    const safetyStock = parseNumberFromLine(contentLines.find((l) => l.includes('安全库存:')))
    let suggestedQty = 1
    if (Number.isFinite(currentStock) && Number.isFinite(safetyStock)) {
      const diff = safetyStock - currentStock
      if (Number.isFinite(diff) && diff > 0) suggestedQty = Math.ceil(diff)
    }

    if (material.materialType === 'PRODUCT') {
      const hasProducingWorkOrder = state.workOrders.some(
        (wo) => wo.productMaterialId === material.id && wo.status === 'PRODUCING',
      )
      if (hasProducingWorkOrder) {
        try {
          const urgeResp = await messageApi.urgeWorkshopProduce(material.id)
          if (urgeResp.code !== 0) return ElMessage.error(urgeResp.message || '催促车间生产失败')
          ElMessage.success('已催促车间生产')
        } catch (e: any) {
          ElMessage.error(e?.response?.data?.message || e?.message || '催促车间生产失败')
        }
        return
      }
      const hasProducedNotInbound = state.workOrders.some(
        (wo) => wo.productMaterialId === material.id && wo.status === 'DONE',
      )
      if (hasProducedNotInbound) {
        try {
          const urgeResp = await messageApi.urgeWarehouseInbound(material.id, 'PRODUCT')
          if (urgeResp.code !== 0) return ElMessage.error(urgeResp.message || '催促仓库入库失败')
          ElMessage.success('已催促仓库入库')
        } catch (e: any) {
          ElMessage.error(e?.response?.data?.message || e?.message || '催促仓库入库失败')
        }
        return
      }

      const firstCustomer = state.customers[0]
      if (!firstCustomer?.id) {
        ElMessage.error('请先在“客户”页面添加客户')
        return
      }
      const now = new Date()
      const yyyy = now.getFullYear()
      const mm = String(now.getMonth() + 1).padStart(2, '0')
      const dd = String(now.getDate()).padStart(2, '0')
      const hh = String(now.getHours()).padStart(2, '0')
      const mi = String(now.getMinutes()).padStart(2, '0')
      const ss = String(now.getSeconds()).padStart(2, '0')
      const replenishOrderNo = `RP${yyyy}${mm}${dd}${hh}${mi}${ss}`
      const deliveryDate = `${yyyy}-${mm}-${dd}`
      try {
        const resp = await orderApi.createOrder({
          orderNo: replenishOrderNo,
          customerId: firstCustomer.id,
          productMaterialId: material.id,
          qty: suggestedQty,
          deliveryDate,
          urgent: false,
          pinned: false,
        })
        if (resp.code !== 0) return ElMessage.error(resp.message || '创建补产任务失败')
        await onOrderRefresh()
        activeTab.value = 'workOrders'
        ElMessage.success('已通知车间补产，生产完成后按正常流程入库（不发货）')
      } catch (e: any) {
        ElMessage.error(e?.response?.data?.message || e?.message || '创建补产任务失败')
      }
      return
    }

    const hasReceivingPending = await hasReceivingPurchasePending(material.id)
    if (hasReceivingPending) {
      try {
        const urgeResp = await messageApi.urgeWarehouseInbound(material.id, 'RAW')
        if (urgeResp.code !== 0) return ElMessage.error(urgeResp.message || '催促仓库入库失败')
        ElMessage.success('已催促仓库入库')
      } catch (e: any) {
        ElMessage.error(e?.response?.data?.message || e?.message || '催促仓库入库失败')
      }
      return
    }

    activeTab.value = 'purchase'
    await nextTick()
    openPurchaseDialog()
    purchaseForm.remark = `库存预警补货 (${material.materialCode})`
    pendingStockLowPurchaseMsgId.value = msg.id
    try {
      const supplierResp = await baseApi.listSuppliersByRawMaterial(material.id)
      if (supplierResp.code === 0) {
        const firstSupplier = supplierResp.data?.[0]
        if (firstSupplier?.id != null) purchaseForm.supplierId = firstSupplier.id
      }
    } catch {
      // ignore
    }
    purchaseForm.lines = [{ materialId: material.id, qty: suggestedQty, price: undefined, remark: undefined }]
  }

  function stockLowActionLabel(msg: MessageNotice): string {
    const materialCode = msg.relatedId || ''
    const material = state.materials.find((m) => m.materialCode === materialCode)
    if (material?.materialType === 'PRODUCT') {
      const hasProducing = state.workOrders.some(
        (wo) => wo.productMaterialId === material.id && wo.status === 'PRODUCING',
      )
      if (hasProducing) return '催促车间生产'
      const hasProducedNotInbound = state.workOrders.some(
        (wo) => wo.productMaterialId === material.id && wo.status === 'DONE',
      )
      return hasProducedNotInbound ? '催促仓库入库' : '通知车间生产'
    }
    const hasReceiving = state.purchaseOrders.some((po) => po.status === 'RECEIVING')
    return hasReceiving ? '催促仓库入库' : '创建采购单'
  }

  watch(
    () => state.messages,
    async (messages) => {
      const capacityMessages = messages.filter(
        (m) => m.noticeType === 'CAPACITY_WARN_PRODUCT' || m.noticeType === 'CAPACITY_WARN_RAW',
      )
      for (const msg of capacityMessages) {
        if (shownCapacityWarningIds.has(msg.id)) continue
        shownCapacityWarningIds.add(msg.id)
        await ElMessageBox.alert(msg.content || msg.title || '库存接近上限，请及时处理', '容量预警', {
          type: 'warning',
          confirmButtonText: '我知道了',
        })
      }
    },
    { immediate: true, deep: true },
  )

  return {
    pendingStockLowPurchaseMsgId,
    messagesReminderCount,
    onWorkOrderRemind,
    closePurchaseDialog,
    onAfterCreateOrderByMessage,
    submitPurchaseOrderFromStockLowMsg,
    onCreatePurchaseFromRequest,
    onCreatePurchaseFromStockLow,
    stockLowActionLabel,
  }
}

