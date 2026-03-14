/**
 * 消息操作组合式函数
 */
import { ElMessage, ElMessageBox } from 'element-plus'
import * as messageApi from '@/api/message'
import * as purchaseApi from '@/api/purchase'

interface UseMessageOperationsOptions {
  loadMessages: () => Promise<void>
  loadPurchaseOrders: () => Promise<void>
  loadStocks: () => Promise<void>
}

/**
 * Notes:
 * - 消息和采购操作逻辑封装
 *
 * Args:
 * - options: 依赖项
 *
 * Returns:
 * - onMarkMessageRead: 标记消息已读
 * - onScanMessages: 扫描消息
 * - onReceivePurchase: 采购收货
 */
export function useMessageOperations(options: UseMessageOperationsOptions) {
  const { loadMessages, loadPurchaseOrders, loadStocks } = options

  async function onMarkMessageRead(row: messageApi.MessageNotice) {
    try {
      await messageApi.markRead(row.id)
      row.read = true
    } catch (e: any) {
      ElMessage.error(e?.message || '操作失败')
    }
  }

  async function onScanMessages() {
    try {
      await messageApi.scanMessages()
      ElMessage.success('已触发扫描')
      await loadMessages()
    } catch (e: any) {
      ElMessage.error(e?.message || '扫描失败')
    }
  }

  async function onReceivePurchase(po: purchaseApi.PurchaseOrder) {
    try {
      await ElMessageBox.confirm(`确认将采购单 ${po.poNo} 全部收货并入库吗？`, '提示', { type: 'warning' })
      await purchaseApi.receiveAll(po.id)
      ElMessage.success('收货完成')
      await loadPurchaseOrders()
      await loadStocks()
    } catch {
      // ignore
    }
  }

  return {
    onMarkMessageRead,
    onScanMessages,
    onReceivePurchase,
  }
}
