export function orderStatusLabel(status?: string | null): string {
  if (status === 'PENDING') return '待处理'
  if (status === 'PRODUCING') return '生产中'
  if (status === 'COMPLETED') return '已制成'
  if (status === 'SHIPPED') return '已发货'
  if (status === 'DONE') return '已完成'
  return status ?? ''
}

export function workOrderStatusLabel(status?: string | null): string {
  if (status === 'TO_PRODUCE') return '待生产'
  if (status === 'PRODUCING') return '生产中'
  if (status === 'DONE') return '已完成'
  if (status === 'STORED') return '已入库'
  return status ?? ''
}

export function purchaseStatusLabel(status?: string | null): string {
  if (status === 'CREATED') return '已创建'
  if (status === 'RECEIVING') return '收货中'
  if (status === 'DONE') return '已完成'
  return status ?? ''
}

export function deviceStatusLabel(status?: string | null): string {
  if (status === 'IDLE') return '空闲'
  if (status === 'RUNNING') return '运行中'
  if (status === 'MAINTENANCE') return '维护中'
  if (status === 'BROKEN') return '故障'
  return status ?? ''
}

export function levelLabel(level?: string | null): string {
  if (level === 'INFO') return '提示'
  if (level === 'WARN') return '警告'
  if (level === 'ERROR') return '错误'
  return level ?? ''
}

export function materialTypeLabel(type?: string | null): string {
  if (type === 'RAW') return '原材料'
  if (type === 'PRODUCT') return '成品'
  return type ?? ''
}

export function noticeTypeLabel(type?: string | null): string {
  if (type === 'STOCK_LOW') return '库存预警'
  if (type === 'WO_OVERDUE') return '工单超期'
  return type ?? ''
}

export function bizTypeLabel(type?: string | null): string {
  if (type === 'PURCHASE_IN') return '采购入库'
  if (type === 'PRODUCTION_IN') return '生产入库'
  if (type === 'PRODUCTION_OUT') return '生产领料'
  if (type === 'SALES_OUT') return '销售出库'
  if (type === 'ADJUST') return '盘点调整'
  return type ?? ''
}

export function genPurchaseNo(): string {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  const s = String(d.getSeconds()).padStart(2, '0')
  return `PO${y}${m}${day}${h}${mi}${s}`
}
