/**
 * 车间端类型定义
 */
import type { ProductionWorkOrder, ProductionReport } from '@/api/production'
import type { Material, BomLine } from '@/api/base'

/** 车间端状态数据 */
export interface WorkshopState {
  workOrders: ProductionWorkOrder[]
  allWorkOrders: ProductionWorkOrder[]
  reports: ProductionReport[]
  reasons: BadReason[]
  materials: Material[]
  bomMap: Record<number, BomLine[]>
}

/** 不良原因 */
export interface BadReason {
  code: string
  text: string
}

/** 报工表单 */
export interface ReportForm {
  workOrderNo: string
  processName: string
  goodQty: number
  badQty: number
  badReasonCode: string
  badReasonText: string
}

/** BOM 编辑行 */
export interface BomEditLine {
  materialId: number | null
  qty: number
}

/** 车间端Tab类型 */
export type WorkshopTabName = 'kanban' | 'pending' | 'report' | 'history' | 'bom'
