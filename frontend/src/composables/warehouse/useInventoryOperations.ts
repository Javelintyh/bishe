/**
 * 库存操作组合式函数
 */
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import * as inventoryApi from '@/api/inventory'
import * as messageApi from '@/api/message'
import * as productionApi from '@/api/production'
import * as orderApi from '@/api/order'
import type {
  InboundForm,
  SalesOutboundForm,
  PurchaseInboundForm,
  OutboundForm,
  AdjustForm,
} from '@/types/warehouse'
import { BizType } from '@/constants/warehouse'

interface UseInventoryOperationsOptions {
  loadData: () => Promise<void>
  getStockQty: (materialId: number) => number
  getMaterialName: (materialId: number) => string
  getWorkOrderBomItems: (wo: productionApi.ProductionWorkOrder) => {
    materialId: number
    requiredQty: number
    stockQty: number
    shortageQty: number
  }[]
  pendingOutboundWorkOrders: { value: productionApi.ProductionWorkOrder[] }
  pendingInboundWorkOrders: { value: productionApi.ProductionWorkOrder[] }
  getAllWorkOrders: () => productionApi.ProductionWorkOrder[]
  getOrders: () => orderApi.OrderVO[]
}

/**
 * Notes:
 * - 库存出入库操作逻辑封装
 *
 * Args:
 * - options (UseInventoryOperationsOptions): 操作所需的依赖项
 *
 * Returns:
 * - 各类表单和操作函数
 */
export function useInventoryOperations(options: UseInventoryOperationsOptions) {
  const {
    loadData,
    getStockQty,
    getMaterialName,
    getWorkOrderBomItems,
    pendingOutboundWorkOrders,
    pendingInboundWorkOrders,
    getAllWorkOrders,
    getOrders,
  } = options

  const inboundForm = reactive<InboundForm>({
    workOrderNo: '',
    materialId: null,
    qty: 1,
  })

  const salesOutboundForm = reactive<SalesOutboundForm>({
    materialId: null,
    qty: 1,
    bizId: '',
  })

  const purchaseInboundForm = reactive<PurchaseInboundForm>({
    materialId: null,
    qty: 1,
    bizId: '',
  })

  const outboundForm = reactive<OutboundForm>({
    workOrderNo: '',
    materialId: null,
    qty: 1,
  })

  const adjustForm = reactive<AdjustForm>({
    materialId: null,
    adjustQty: 0,
    remark: '',
  })

  function resetInboundForm() {
    inboundForm.workOrderNo = ''
    inboundForm.materialId = null
    inboundForm.qty = 1
  }

  function resetSalesOutboundForm() {
    salesOutboundForm.materialId = null
    salesOutboundForm.qty = 1
    salesOutboundForm.bizId = ''
  }

  function resetPurchaseInboundForm() {
    purchaseInboundForm.materialId = null
    purchaseInboundForm.qty = 1
    purchaseInboundForm.bizId = ''
  }

  function resetOutboundForm() {
    outboundForm.workOrderNo = ''
    outboundForm.materialId = null
    outboundForm.qty = 1
  }

  function resetAdjustForm() {
    adjustForm.materialId = null
    adjustForm.adjustQty = 0
    adjustForm.remark = ''
  }

  function onSelectInboundWorkOrder(workOrderNo: string) {
    const wo = pendingInboundWorkOrders.value.find((w) => w.workOrderNo === workOrderNo)
    if (wo) {
      inboundForm.materialId = wo.productMaterialId
      inboundForm.qty = wo.qty
    }
  }

  function onSelectOutboundWorkOrder(workOrderNo: string) {
    const wo = pendingOutboundWorkOrders.value.find((w) => w.workOrderNo === workOrderNo)
    if (wo) {
      outboundForm.materialId = wo.productMaterialId
      outboundForm.qty = wo.qty
    }
  }

  async function onQuickOutbound(wo: productionApi.ProductionWorkOrder) {
    const productStock = getStockQty(wo.productMaterialId)

    if (productStock >= wo.qty) {
      try {
        await inventoryApi.outbound({
          materialId: wo.productMaterialId,
          qty: wo.qty,
          bizType: BizType.SALES_OUT,
          bizId: wo.workOrderNo,
        })
        await productionApi.updateWorkOrderForProduce(wo.id, 'STORED', 0)

        const relatedOrder = getOrders().find((o) => o.id === wo.orderId)
        if (relatedOrder) {
          await orderApi.updateOrderStatus(relatedOrder.id, 'SHIPPED')
        }

        ElMessage.success('成品出库成功，订单已发货')
        await loadData()
      } catch (e: any) {
        ElMessage.error(e.response?.data?.message || '出库失败')
      }
      return
    }

    const bomItems = getWorkOrderBomItems(wo)
    if (bomItems.length === 0) {
      ElMessage.warning('该产品无BOM配置，无法出库原材料')
      return
    }

    const shortages = bomItems.filter((item) => item.shortageQty > 0)
    if (shortages.length > 0) {
      ElMessage.error(
        `原材料不足，无法出库: ${shortages.map((s) => `${getMaterialName(s.materialId)}缺${s.shortageQty}`).join(', ')}`,
      )
      return
    }

    try {
      if (productStock > 0) {
        await inventoryApi.outbound({
          materialId: wo.productMaterialId,
          qty: productStock,
          bizType: BizType.SALES_OUT,
          bizId: wo.workOrderNo,
        })
      }

      for (const item of bomItems) {
        if (item.requiredQty > 0) {
          await inventoryApi.outbound({
            materialId: item.materialId,
            qty: item.requiredQty,
            bizType: BizType.PRODUCTION_OUT,
            bizId: wo.workOrderNo,
          })
        }
      }

      const needProduceQty = wo.qty - productStock
      await productionApi.updateWorkOrderForProduce(wo.id, 'PRODUCING', needProduceQty)

      const relatedOrder = getOrders().find((o) => o.id === wo.orderId)
      if (relatedOrder) {
        await orderApi.updateOrderStatus(relatedOrder.id, 'PRODUCING')
      }

      if (productStock > 0) {
        ElMessage.success(`已出库成品${productStock}个，原材料已发往车间生产${needProduceQty}个`)
      } else {
        ElMessage.success('原材料出库成功，已发往车间生产')
      }
      await loadData()
    } catch (e: any) {
      ElMessage.error(e.response?.data?.message || '出库失败')
    }
  }

  async function onQuickInbound(wo: productionApi.ProductionWorkOrder) {
    try {
      await inventoryApi.inbound({
        materialId: wo.productMaterialId,
        qty: wo.qty,
        bizType: BizType.PRODUCTION_IN,
        bizId: wo.workOrderNo,
      })
      // 入库后工单进入“已完成”态，由订单状态与工单状态统一驱动
      await productionApi.updateWorkOrderStatus(wo.id, 'COMPLETED')

      const relatedOrder = getOrders().find((o) => o.id === wo.orderId)
      if (relatedOrder) {
        await orderApi.updateOrderStatus(relatedOrder.id, 'COMPLETED')
      }

      ElMessage.success('成品入库成功')
      await loadData()
    } catch (e: any) {
      ElMessage.error(e.response?.data?.message || '入库失败')
    }
  }

  async function onShipOrder(order: orderApi.OrderVO) {
    const relatedWorkOrder = getAllWorkOrders().find((w) => w.orderId === order.id)
    if (!relatedWorkOrder) {
      ElMessage.error('未找到关联工单')
      return
    }

    try {
      const shipQty = relatedWorkOrder.qty
      if (shipQty > 0) {
        await inventoryApi.outbound({
          materialId: relatedWorkOrder.productMaterialId,
          qty: shipQty,
          bizType: BizType.SALES_OUT,
          bizId: order.orderNo,
        })
      }
      await orderApi.updateOrderStatus(order.id, 'SHIPPED')
      ElMessage.success('发货成功')
      await loadData()
    } catch (e: any) {
      ElMessage.error(e.response?.data?.message || '发货失败')
    }
  }

  async function onInbound() {
    if (!inboundForm.materialId || inboundForm.qty <= 0) {
      ElMessage.warning('请填写完整信息')
      return
    }
    try {
      await inventoryApi.inbound({
        materialId: inboundForm.materialId,
        qty: inboundForm.qty,
        bizType: BizType.PRODUCTION_IN,
        bizId: inboundForm.workOrderNo || undefined,
      })
      ElMessage.success('入库成功')
      resetInboundForm()
      await loadData()
    } catch (e: any) {
      ElMessage.error(e.response?.data?.message || '入库失败')
    }
  }

  async function onSalesOutbound() {
    if (!salesOutboundForm.materialId || salesOutboundForm.qty <= 0) {
      ElMessage.warning('请填写完整信息')
      return
    }
    const stock = getStockQty(salesOutboundForm.materialId)
    if (salesOutboundForm.qty > stock) {
      ElMessage.error(`库存不足，当前库存: ${stock}`)
      return
    }
    try {
      await inventoryApi.outbound({
        materialId: salesOutboundForm.materialId,
        qty: salesOutboundForm.qty,
        bizType: BizType.SALES_OUT,
        bizId: salesOutboundForm.bizId || undefined,
      })
      ElMessage.success('出库成功')
      resetSalesOutboundForm()
      await loadData()
    } catch (e: any) {
      ElMessage.error(e.response?.data?.message || '出库失败')
    }
  }

  async function onPurchaseInbound() {
    if (!purchaseInboundForm.materialId || purchaseInboundForm.qty <= 0) {
      ElMessage.warning('请填写完整信息')
      return
    }
    try {
      // 为了流程清晰：这里保留手动入库，用于无采购单场景
      await inventoryApi.inbound({
        materialId: purchaseInboundForm.materialId,
        qty: purchaseInboundForm.qty,
        bizType: BizType.PURCHASE_IN,
        bizId: purchaseInboundForm.bizId || undefined,
      })
      ElMessage.success('入库成功')
      resetPurchaseInboundForm()
      await loadData()
    } catch (e: any) {
      ElMessage.error(e.response?.data?.message || '入库失败')
    }
  }

  async function onRequestPurchase(materialId: number) {
    try {
      await messageApi.createPurchaseRequest({
        materialId,
        remark: '',
        qty: String(purchaseInboundForm.qty > 0 ? purchaseInboundForm.qty : ''),
      })
      ElMessage.success('已发送采购请求给管理员')
    } catch (e: any) {
      ElMessage.error(e.response?.data?.message || '请求失败')
    }
  }

  async function onOutbound() {
    if (!outboundForm.materialId || outboundForm.qty <= 0) {
      ElMessage.warning('请填写完整信息')
      return
    }
    const stock = getStockQty(outboundForm.materialId)
    if (outboundForm.qty > stock) {
      ElMessage.error(`库存不足，当前库存: ${stock}`)
      return
    }
    try {
      await inventoryApi.outbound({
        materialId: outboundForm.materialId,
        qty: outboundForm.qty,
        bizType: BizType.PRODUCTION_OUT,
        bizId: outboundForm.workOrderNo || undefined,
      })
      ElMessage.success('出库成功')
      resetOutboundForm()
      await loadData()
    } catch (e: any) {
      ElMessage.error(e.response?.data?.message || '出库失败')
    }
  }

  async function onAdjust() {
    if (!adjustForm.materialId) {
      ElMessage.warning('请选择物料')
      return
    }
    if (adjustForm.adjustQty === 0) {
      ElMessage.warning('调整数量不能为0')
      return
    }
    try {
      if (adjustForm.adjustQty > 0) {
        await inventoryApi.inbound({
          materialId: adjustForm.materialId,
          qty: adjustForm.adjustQty,
          bizType: BizType.ADJUST,
        })
      } else {
        await inventoryApi.outbound({
          materialId: adjustForm.materialId,
          qty: Math.abs(adjustForm.adjustQty),
          bizType: BizType.ADJUST,
        })
      }
      ElMessage.success('盘点调整成功')
      resetAdjustForm()
      await loadData()
    } catch (e: any) {
      ElMessage.error(e.response?.data?.message || '调整失败')
    }
  }

  return {
    inboundForm,
    salesOutboundForm,
    purchaseInboundForm,
    outboundForm,
    adjustForm,
    onSelectInboundWorkOrder,
    onSelectOutboundWorkOrder,
    onQuickOutbound,
    onQuickInbound,
    onShipOrder,
    onInbound,
    onSalesOutbound,
    onPurchaseInbound,
    onRequestPurchase,
    onOutbound,
    onAdjust,
  }
}
