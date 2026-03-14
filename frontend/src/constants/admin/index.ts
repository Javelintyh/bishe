/**
 * 管理端常量配置
 */

// 导出通用常量
export * from '../common'

/** 采购状态枚举 */
export const PurchaseStatus = {
  CREATED: 'CREATED',
  RECEIVING: 'RECEIVING',
  DONE: 'DONE',
} as const

export type PurchaseStatusCode = (typeof PurchaseStatus)[keyof typeof PurchaseStatus]

/** 采购状态标签映射 */
export const PURCHASE_STATUS_LABELS: Record<PurchaseStatusCode, string> = {
  [PurchaseStatus.CREATED]: '已创建',
  [PurchaseStatus.RECEIVING]: '收货中',
  [PurchaseStatus.DONE]: '已完成',
}

/** 设备状态枚举 */
export const DeviceStatus = {
  IDLE: 'IDLE',
  RUNNING: 'RUNNING',
  MAINTENANCE: 'MAINTENANCE',
  BROKEN: 'BROKEN',
} as const

export type DeviceStatusCode = (typeof DeviceStatus)[keyof typeof DeviceStatus]

/** 设备状态标签映射 */
export const DEVICE_STATUS_LABELS: Record<DeviceStatusCode, string> = {
  [DeviceStatus.IDLE]: '空闲',
  [DeviceStatus.RUNNING]: '运行中',
  [DeviceStatus.MAINTENANCE]: '维护中',
  [DeviceStatus.BROKEN]: '故障',
}

/** 消息级别枚举 */
export const MessageLevel = {
  INFO: 'INFO',
  WARN: 'WARN',
  ERROR: 'ERROR',
} as const

export type MessageLevelCode = (typeof MessageLevel)[keyof typeof MessageLevel]

/** 消息级别标签映射 */
export const MESSAGE_LEVEL_LABELS: Record<MessageLevelCode, string> = {
  [MessageLevel.INFO]: '提示',
  [MessageLevel.WARN]: '警告',
  [MessageLevel.ERROR]: '错误',
}

/** 通知类型枚举 */
export const NoticeType = {
  STOCK_LOW: 'STOCK_LOW',
  WO_OVERDUE: 'WO_OVERDUE',
} as const

export type NoticeTypeCode = (typeof NoticeType)[keyof typeof NoticeType]

/** 通知类型标签映射 */
export const NOTICE_TYPE_LABELS: Record<NoticeTypeCode, string> = {
  [NoticeType.STOCK_LOW]: '库存预警',
  [NoticeType.WO_OVERDUE]: '工单超期',
}

/** 状态标签获取函数 */
export function purchaseStatusLabel(status?: string | null): string {
  if (!status) return ''
  return PURCHASE_STATUS_LABELS[status as PurchaseStatusCode] ?? status
}

export function deviceStatusLabel(status?: string | null): string {
  if (!status) return ''
  return DEVICE_STATUS_LABELS[status as DeviceStatusCode] ?? status
}

export function levelLabel(level?: string | null): string {
  if (!level) return ''
  return MESSAGE_LEVEL_LABELS[level as MessageLevelCode] ?? level
}

export function noticeTypeLabel(type?: string | null): string {
  if (!type) return ''
  return NOTICE_TYPE_LABELS[type as NoticeTypeCode] ?? type
}
