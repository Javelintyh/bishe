/**
 * 管理端类型定义
 */
import type { UserVO } from '@/api/sys'
import type { Material, Customer, Supplier } from '@/api/base'
import type { OrderVO } from '@/api/order'
import type { ProductionWorkOrder } from '@/api/production'
import type { InventoryStock } from '@/api/inventory'
import type { QualityReason } from '@/api/quality'
import type { PurchaseOrder } from '@/api/purchase'
import type { DeviceAsset } from '@/api/device'
import type { MessageNotice } from '@/api/message'
import type { OrderSummaryDTO, ProductionSummaryDTO, InventoryTurnoverDTO } from '@/api/report'

/** 管理端状态数据 */
export interface AdminState {
  users: UserVO[]
  materials: Material[]
  customers: Customer[]
  suppliers: Supplier[]
  orders: OrderVO[]
  workOrders: ProductionWorkOrder[]
  stocks: InventoryStock[]
  reasons: QualityReason[]
  purchaseOrders: PurchaseOrder[]
  devices: DeviceAsset[]
  messages: MessageNotice[]
  orderSummary: OrderSummaryDTO | null
  productionSummary: ProductionSummaryDTO | null
  inventoryTurnover: InventoryTurnoverDTO | null
}

/** 采购单表单 */
export interface PurchaseForm {
  poNo: string
  supplierId: number | undefined
  expectedDate: string
  remark: string
  lines: PurchaseFormLine[]
}

/** 采购单明细行 */
export interface PurchaseFormLine {
  materialId: number
  qty: number
  price?: number
  remark?: string
}

/** 采购单明细行输入 */
export interface PurchaseFormLineInput {
  materialId: number | undefined
  qty: number
  price: number | undefined
  remark: string
}

/** 管理端Tab类型 */
export type AdminTabName =
  | 'overview'
  | 'users'
  | 'materials'
  | 'customers'
  | 'suppliers'
  | 'orders'
  | 'workOrders'
  | 'inventory'
  | 'quality'
  | 'purchase'
  | 'devices'
  | 'messages'
