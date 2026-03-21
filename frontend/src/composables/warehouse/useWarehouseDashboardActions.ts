import { ref, type Ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as messageApi from '@/api/message'
import * as purchaseApi from '@/api/purchase'
import * as orderApi from '@/api/order'
import * as baseApi from '@/api/base'
import type { MessageNotice } from '@/api/message'
import type { Material } from '@/api/base'
import type { ProductionWorkOrder } from '@/api/production'
import type { WarehouseTabName, PurchaseInboundForm, OutboundForm, SalesOutboundForm } from '@/types/warehouse'

type InboundFromMessagePayload = {
  msg: MessageNotice
  poId: number
  poNo: string
  prefill: {
    materialId: number | null
    qty: number
  }
}

type UseWarehouseDashboardActionsOptions = {
  activeTab: Ref<WarehouseTabName>
  state: {
    materials: Material[]
    allWorkOrders: ProductionWorkOrder[]
  }
  pendingOutboundWorkOrders: Ref<ProductionWorkOrder[]>
  productMaterials: Ref<Material[]>
  rawMaterials: Ref<Material[]>
  getStockQty: (materialId: number) => number
  isWorkOrderStockSufficient: (wo: ProductionWorkOrder) => boolean
  loadData: () => Promise<void>
  onRequestPurchase: (materialId: number) => Promise<void>
  onSalesOutbound: () => Promise<void>
  onOutbound: () => Promise<void>
  purchaseInboundForm: PurchaseInboundForm
  outboundForm: OutboundForm
  salesOutboundForm: SalesOutboundForm
}

export function useWarehouseDashboardActions(options: UseWarehouseDashboardActionsOptions) {
  const {
    activeTab,
    state,
    pendingOutboundWorkOrders,
    productMaterials,
    rawMaterials,
    getStockQty,
    isWorkOrderStockSufficient,
    loadData,
    onRequestPurchase,
    onSalesOutbound,
    onOutbound,
    purchaseInboundForm,
    outboundForm,
    salesOutboundForm,
  } = options

  const currentInboundPurchaseMsg = ref<MessageNotice | null>(null)
  const purchaseMessagesRefreshToken = ref(0)
  const shownCapacityWarningIds = new Set<number>()

  function parseNumberFromLine(line: string | undefined): number {
    if (!line) return NaN
    const m = line.match(/-?\d+(\.\d+)?/)
    if (!m) return NaN
    return Number(m[0])
  }

  function handleCapacityWarningJump(msg: MessageNotice) {
    const lines = String(msg.content || '').split('\n').map((l) => l.trim())
    const remaining = parseNumberFromLine(lines.find((l) => l.includes('剩余容量')))

    if (msg.noticeType === 'CAPACITY_WARN_PRODUCT') {
      activeTab.value = 'product'
      const needOutbound = Number.isFinite(remaining) ? Math.max(1, Math.ceil(101 - remaining)) : 1
      const products = productMaterials.value
        .map((m) => ({ id: m.id, stock: Number(getStockQty(m.id) || 0) }))
        .filter((x) => x.stock > 0)
        .sort((a, b) => b.stock - a.stock)
      const target = products[0]
      if (target) {
        salesOutboundForm.materialId = target.id
        salesOutboundForm.qty = Math.max(1, Math.min(target.stock, needOutbound))
      }
      return
    }

    if (msg.noticeType === 'CAPACITY_WARN_RAW') {
      activeTab.value = 'rawMaterial'
      const codeLine = lines.find((l) => l.startsWith('物料编码:'))
      const code = codeLine?.split('物料编码:')[1]?.trim()
      const needOutbound = Number.isFinite(remaining) ? Math.max(1, Math.ceil(1001 - remaining)) : 1
      if (code) {
        const material = rawMaterials.value.find((m) => m.materialCode === code)
        if (material?.id) {
          outboundForm.materialId = material.id
          const stock = Number(getStockQty(material.id) || 0)
          outboundForm.qty = Math.max(1, Math.min(stock > 0 ? stock : needOutbound, needOutbound))
        }
      }
    }
  }

  async function popupCapacityWarningsAndMaybeJump() {
    try {
      const resp = await messageApi.listMessages(true)
      if (resp.code !== 0) return
      const warnings = (resp.data ?? []).filter(
        (m) => m.noticeType === 'CAPACITY_WARN_PRODUCT' || m.noticeType === 'CAPACITY_WARN_RAW',
      )
      for (const msg of warnings) {
        if (shownCapacityWarningIds.has(msg.id)) continue
        shownCapacityWarningIds.add(msg.id)
        const isProduct = msg.noticeType === 'CAPACITY_WARN_PRODUCT'
        const action = await ElMessageBox.confirm(
          msg.content || msg.title || '库存接近上限，请及时处理',
          '容量预警',
          {
            type: 'warning',
            confirmButtonText: isProduct ? '去成品出库' : '去原材料出库',
            cancelButtonText: '稍后处理',
            distinguishCancelAndClose: true,
          },
        ).catch(() => 'cancel')
        if (action === 'confirm') handleCapacityWarningJump(msg)
      }
    } catch {
      // ignore
    }
  }

  async function onWarehouseInboundFromMessage(payload: InboundFromMessagePayload) {
    currentInboundPurchaseMsg.value = payload.msg
    purchaseInboundForm.materialId = payload.prefill.materialId
    purchaseInboundForm.qty = payload.prefill.qty
    purchaseInboundForm.bizId = String(payload.poId)
    activeTab.value = 'rawMaterial'
  }

  async function onQuickOutboundFromWorkOrderMessage(payload: { msg: MessageNotice; workOrderNo: string }) {
    const { workOrderNo } = payload
    if (!workOrderNo) {
      ElMessage.error('消息缺少工单号信息')
      return
    }
    const wo = pendingOutboundWorkOrders.value.find((w) => w.workOrderNo === workOrderNo)
    if (!wo) {
      ElMessage.warning('未找到可处理的工单（可能已不在待出库列表）')
      return
    }
    activeTab.value = 'workOrders'
    if (!isWorkOrderStockSufficient(wo)) {
      ElMessage.warning(`已跳转到“工单待出库”，但工单 ${workOrderNo} 原材料不足，按钮会不可点。`)
    } else {
      ElMessage.info(`已跳转到“工单待出库”，请找到工单 ${workOrderNo} 点击“一键出库”。`)
    }
  }

  async function onCreateProductionFromStockLowMessage(payload: { msg: MessageNotice }) {
    const msg = payload.msg
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

    const hasReceivingPurchasePending = async (mid: number): Promise<boolean> => {
      try {
        const poResp = await purchaseApi.listPurchaseOrders('RECEIVING')
        const receiving = poResp.data ?? []
        for (const po of receiving) {
          const detailResp = await purchaseApi.getPurchaseOrderDetails(po.id)
          const details = detailResp.data ?? []
          const hit = details.some((d) => d.materialId === mid && Number(d.qty) > Number(d.receivedQty ?? 0))
          if (hit) return true
        }
      } catch {
        // ignore
      }
      return false
    }

    if (material.materialType !== 'PRODUCT') {
      const hasReceivingPending = await hasReceivingPurchasePending(material.id)
      if (hasReceivingPending) {
        try {
          const urgeResp = await messageApi.urgeWarehouseInbound(material.id, 'RAW')
          if (urgeResp.code !== 0) {
            ElMessage.error(urgeResp.message || '催促仓库入库失败')
            return
          }
          ElMessage.success('已催促仓库入库')
        } catch (e: any) {
          ElMessage.error(e?.response?.data?.message || e?.message || '催促仓库入库失败')
        }
        return
      }
      try {
        await onRequestPurchase(material.id)
        await loadData()
        purchaseMessagesRefreshToken.value++
        ElMessage.success('已提交申请采购')
      } catch (e: any) {
        ElMessage.error(e?.response?.data?.message || e?.message || '申请采购失败')
      }
      return
    }

    const hasProducingWorkOrder = state.allWorkOrders.some(
      (wo) => wo.productMaterialId === material.id && wo.status === 'PRODUCING',
    )
    if (hasProducingWorkOrder) {
      try {
        const urgeResp = await messageApi.urgeWorkshopProduce(material.id)
        if (urgeResp.code !== 0) {
          ElMessage.error(urgeResp.message || '催促车间生产失败')
          return
        }
        ElMessage.success('已催促车间生产')
      } catch (e: any) {
        ElMessage.error(e?.response?.data?.message || e?.message || '催促车间生产失败')
      }
      return
    }

    const hasProducedNotInbound = state.allWorkOrders.some(
      (wo) => wo.productMaterialId === material.id && wo.status === 'DONE',
    )
    if (hasProducedNotInbound) {
      try {
        const urgeResp = await messageApi.urgeWarehouseInbound(material.id, 'PRODUCT')
        if (urgeResp.code !== 0) {
          ElMessage.error(urgeResp.message || '催促仓库入库失败')
          return
        }
        ElMessage.success('已催促仓库入库')
      } catch (e: any) {
        ElMessage.error(e?.response?.data?.message || e?.message || '催促仓库入库失败')
      }
      return
    }

    const currentStock = parseNumberFromLine(contentLines.find((l) => l.includes('当前库存:')))
    const safetyStock = parseNumberFromLine(contentLines.find((l) => l.includes('安全库存:')))
    let suggestedQty = 1
    if (Number.isFinite(currentStock) && Number.isFinite(safetyStock)) {
      const diff = safetyStock - currentStock
      if (Number.isFinite(diff) && diff > 0) suggestedQty = Math.ceil(diff)
    }

    try {
      const customerResp = await baseApi.listCustomers()
      const firstCustomer = customerResp.data?.[0]
      if (!firstCustomer?.id) {
        ElMessage.error('缺少客户基础数据，无法创建补产任务')
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

      const createResp = await orderApi.createOrder({
        orderNo: replenishOrderNo,
        customerId: firstCustomer.id,
        productMaterialId: material.id,
        qty: suggestedQty,
        deliveryDate,
        urgent: false,
        pinned: false,
      })
      if (createResp.code !== 0) {
        ElMessage.error(createResp.message || '通知车间生产失败')
        return
      }
      await loadData()
      purchaseMessagesRefreshToken.value++
      ElMessage.success('已通知车间生产')
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || e?.message || '通知车间生产失败')
    }
  }

  async function onHandleWarehouseInboundUrge(payload: { msg: MessageNotice }) {
    const lines = String(payload.msg.content || '').split('\n').map((l) => l.trim())
    const sceneLine = lines.find((l) => l.startsWith('场景:'))
    const scene = sceneLine?.split('场景:')[1]?.trim()
    activeTab.value = scene === 'PRODUCT' ? 'pendingInbound' : 'rawMaterial'
    try {
      await messageApi.markRead(payload.msg.id)
    } catch {
      // ignore
    }
    purchaseMessagesRefreshToken.value++
  }

  async function handleSalesOutboundAndRefreshMessages() {
    await onSalesOutbound()
    purchaseMessagesRefreshToken.value++
  }

  async function handleRawOutboundAndRefreshMessages() {
    await onOutbound()
    purchaseMessagesRefreshToken.value++
  }

  async function handlePurchaseInbound() {
    const poId = purchaseInboundForm.bizId ? Number(purchaseInboundForm.bizId) : null
    if (poId) {
      try {
        const resp = await purchaseApi.receiveAll(poId)
        if (resp.code !== 0) throw new Error(resp.message || '入库失败')
        ElMessage.success('入库完成')
        if (currentInboundPurchaseMsg.value) {
          try {
            await messageApi.markRead(currentInboundPurchaseMsg.value.id)
          } catch {
            // ignore
          }
          currentInboundPurchaseMsg.value = null
        }
        purchaseInboundForm.materialId = null
        purchaseInboundForm.qty = 1
        purchaseInboundForm.bizId = ''
        await loadData()
        purchaseMessagesRefreshToken.value++
      } catch (e: any) {
        ElMessage.error(e?.message || '入库失败')
      }
      return
    }

    if (!purchaseInboundForm.materialId || purchaseInboundForm.qty <= 0) {
      ElMessage.warning('请选择原材料并填写数量')
      return
    }
    await onRequestPurchase(purchaseInboundForm.materialId)
    ElMessage.success('已提交采购申请')
    purchaseInboundForm.materialId = null
    purchaseInboundForm.qty = 1
    purchaseInboundForm.bizId = ''
  }

  return {
    purchaseMessagesRefreshToken,
    popupCapacityWarningsAndMaybeJump,
    onWarehouseInboundFromMessage,
    onQuickOutboundFromWorkOrderMessage,
    onCreateProductionFromStockLowMessage,
    onHandleWarehouseInboundUrge,
    handleCapacityWarningJump,
    handleSalesOutboundAndRefreshMessages,
    handleRawOutboundAndRefreshMessages,
    handlePurchaseInbound,
  }
}

