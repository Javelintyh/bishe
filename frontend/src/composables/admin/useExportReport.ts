/**
 * 报表导出组合式函数
 */
import { ElMessage } from 'element-plus'
import type { OrderVO } from '@/api/order'
import type { ProductionWorkOrder } from '@/api/production'
import type { InventoryStock } from '@/api/inventory'

/**
 * Notes:
 * - 报表导出逻辑封装
 *
 * Returns:
 * - exportCsv: 导出CSV文件
 * - onExportReport: 导出报表
 */
export function useExportReport() {
  function exportCsv(name: string, rows: string[][]) {
    const BOM = '\uFEFF'
    const csv = BOM + rows.map((r) => r.map((c) => `"${String(c).replace(/"/g, '""')}"`).join(',')).join('\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${name}.csv`
    a.click()
    URL.revokeObjectURL(url)
  }

  function onExportReport(
    orders: OrderVO[],
    workOrders: ProductionWorkOrder[],
    stocks: InventoryStock[],
  ) {
    const tabs = [
      {
        name: '订单',
        rows: [['订单号', '客户ID', '状态', '交货期', '产品物料ID', '数量', '工单号'] as string[]].concat(
          orders.map((o) => [
            o.orderNo,
            String(o.customerId),
            o.status ?? '',
            o.deliveryDate ?? '',
            String(o.productMaterialId),
            String(o.qty),
            o.workOrderNo ?? '',
          ]),
        ),
      },
      {
        name: '工单',
        rows: [['工单号', '订单ID', '产品物料ID', '数量', '状态', '计划完成'] as string[]].concat(
          workOrders.map((w) => [
            w.workOrderNo,
            String(w.orderId),
            String(w.productMaterialId),
            String(w.qty),
            w.status ?? '',
            w.dueDate ?? '',
          ]),
        ),
      },
      {
        name: '库存',
        rows: [['物料ID', '数量', '更新时间'] as string[]].concat(
          stocks.map((s) => [String(s.materialId), String(s.qty), s.updatedAt ?? '']),
        ),
      },
    ]
    tabs.forEach((t) => {
      if (t.rows.length > 1) exportCsv(t.name, t.rows)
    })
    ElMessage.success('已导出 CSV')
  }

  return {
    exportCsv,
    onExportReport,
  }
}
