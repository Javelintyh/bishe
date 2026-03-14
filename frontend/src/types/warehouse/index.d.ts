/**
 * 仓库端类型定义
 */
import type { InventoryStock, InventoryRecord } from '@/api/inventory'
import type { Material, BomLine } from '@/api/base'
import type { ProductionWorkOrder } from '@/api/production'
import type { OrderVO } from '@/api/order'

/** BOM物料项（含库存信息） */
export interface BomItem {
  materialId: number
  requiredQty: number
  stockQty: number
  shortageQty: number
}

/** 仓库端状态数据 */
export interface WarehouseState {
  stocks: InventoryStock[]
  materials: Material[]
  records: InventoryRecord[]
  workOrders: ProductionWorkOrder[]
  allWorkOrders: ProductionWorkOrder[]
  orders: OrderVO[]
  bomMap: Record<number, BomLine[]>
}

/** 入库表单 */
export interface InboundForm {
  workOrderNo: string
  materialId: number | null
  qty: number
}

/** 销售出库表单 */
export interface SalesOutboundForm {
  materialId: number | null
  qty: number
  bizId: string
}

/** 采购入库表单 */
export interface PurchaseInboundForm {
  materialId: number | null
  qty: number
  bizId: string
}

/** 原材料出库表单 */
export interface OutboundForm {
  workOrderNo: string
  materialId: number | null
  qty: number
}

/** 盘点调整表单 */
export interface AdjustForm {
  materialId: number | null
  adjustQty: number
  remark: string
}

/** 仓库端Tab类型 */
export type WarehouseTabName =
  | 'stocks'
  | 'todayRecords'
  | 'workOrders'
  | 'pendingInbound'
  | 'pendingShip'
  | 'product'
  | 'rawMaterial'
  | 'adjust'
