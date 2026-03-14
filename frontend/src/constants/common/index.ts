/**
 * 通用常量配置
 */

/** 默认分页大小 */
export const PAGE_SIZE = 5

/** 工单状态枚举 */
export const WorkOrderStatus = {
  TO_PRODUCE: 'TO_PRODUCE',
  PRODUCING: 'PRODUCING',
  DONE: 'DONE',
  STORED: 'STORED',
} as const

export type WorkOrderStatusCode = (typeof WorkOrderStatus)[keyof typeof WorkOrderStatus]

/** 工单状态标签映射 */
export const WORK_ORDER_STATUS_LABELS: Record<WorkOrderStatusCode, string> = {
  [WorkOrderStatus.TO_PRODUCE]: '待生产',
  [WorkOrderStatus.PRODUCING]: '生产中',
  [WorkOrderStatus.DONE]: '已完成',
  [WorkOrderStatus.STORED]: '已入库',
}

/** 订单状态枚举 */
export const OrderStatus = {
  PENDING: 'PENDING',
  PRODUCING: 'PRODUCING',
  COMPLETED: 'COMPLETED',
  SHIPPED: 'SHIPPED',
  DONE: 'DONE',
} as const

export type OrderStatusCode = (typeof OrderStatus)[keyof typeof OrderStatus]

/** 订单状态标签映射 */
export const ORDER_STATUS_LABELS: Record<OrderStatusCode, string> = {
  [OrderStatus.PENDING]: '待处理',
  [OrderStatus.PRODUCING]: '生产中',
  [OrderStatus.COMPLETED]: '已制成',
  [OrderStatus.SHIPPED]: '已发货',
  [OrderStatus.DONE]: '已完成',
}

/** 物料类型枚举 */
export const MaterialType = {
  RAW: 'RAW',
  PRODUCT: 'PRODUCT',
} as const

export type MaterialTypeCode = (typeof MaterialType)[keyof typeof MaterialType]

/** 物料类型标签映射 */
export const MATERIAL_TYPE_LABELS: Record<MaterialTypeCode, string> = {
  [MaterialType.RAW]: '原材料',
  [MaterialType.PRODUCT]: '成品',
}

/** 业务类型枚举 */
export const BizType = {
  PURCHASE_IN: 'PURCHASE_IN',
  PRODUCTION_IN: 'PRODUCTION_IN',
  PRODUCTION_OUT: 'PRODUCTION_OUT',
  SALES_OUT: 'SALES_OUT',
  ADJUST: 'ADJUST',
} as const

export type BizTypeCode = (typeof BizType)[keyof typeof BizType]

/** 业务类型标签映射 */
export const BIZ_TYPE_LABELS: Record<BizTypeCode, string> = {
  [BizType.PURCHASE_IN]: '采购入库',
  [BizType.PRODUCTION_IN]: '生产入库',
  [BizType.PRODUCTION_OUT]: '生产领料',
  [BizType.SALES_OUT]: '销售出库',
  [BizType.ADJUST]: '盘点调整',
}

/** 状态标签获取函数 */
export function orderStatusLabel(status?: string | null): string {
  if (!status) return ''
  return ORDER_STATUS_LABELS[status as OrderStatusCode] ?? status
}

export function workOrderStatusLabel(status?: string | null): string {
  if (!status) return ''
  return WORK_ORDER_STATUS_LABELS[status as WorkOrderStatusCode] ?? status
}

export function materialTypeLabel(type?: string | null): string {
  if (!type) return ''
  return MATERIAL_TYPE_LABELS[type as MaterialTypeCode] ?? type
}

export function bizTypeLabel(type?: string | null): string {
  if (!type) return ''
  return BIZ_TYPE_LABELS[type as BizTypeCode] ?? type
}
