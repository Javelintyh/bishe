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
 * - onMarkPurchased: 管理端标记采购单“已购入”
 * - onReceivePurchaseFromMessage: 仓库端从消息执行入库
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

  async function onMarkPurchased(po: purchaseApi.PurchaseOrder) {
    try {
      await ElMessageBox.confirm(`确认将采购单 ${po.poNo} 标记为“已购入”？`, '提示', { type: 'warning' })
      await purchaseApi.markPurchased(po.id)
      ElMessage.success('已标记为已购入')
      await loadPurchaseOrders()
      await loadMessages()
    } catch {
      // ignore
    }
  }

  async function onReceivePurchaseFromMessage(msg: messageApi.MessageNotice) {
    const poId = Number(msg.relatedId)
    if (!poId) {
      ElMessage.error('消息缺少采购单信息')
      return
    }
    try {
      await ElMessageBox.confirm(`确认将采购单 ${msg.relatedId} 全部入库吗？`, '提示', { type: 'warning' })
      await purchaseApi.receiveAll(poId)
      ElMessage.success('入库完成')
      // 处理消息后标记为已读，确保消息从“未读消息列表”消失
      try {
        await messageApi.markRead(msg.id)
        msg.read = true
      } catch {
        // ignore
      }
      await loadPurchaseOrders()
      await loadStocks()
      await loadMessages()
    } catch {
      // ignore
    }
  }

  return {
    onMarkMessageRead,
    onScanMessages,
    onMarkPurchased,
    onReceivePurchaseFromMessage,
  }
}
