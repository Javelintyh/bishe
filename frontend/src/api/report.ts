import { http, type ApiResponse } from './http'

export type OrderSummaryDTO = {
  totalCount: number
  deliveredCount: number
  onTimeDeliveredCount: number
}

export type ProductionSummaryDTO = {
  totalWorkOrders: number
  doneWorkOrders: number
  producingWorkOrders: number
}

export type InventoryTurnoverDTO = {
  totalInQty: number
  totalOutQty: number
  currentStockQty: number
}

export async function getOrderSummary() {
  const { data } = await http.get<ApiResponse<OrderSummaryDTO>>('/api/report/order-summary')
  return data
}

export async function getProductionSummary() {
  const { data } = await http.get<ApiResponse<ProductionSummaryDTO>>('/api/report/production-summary')
  return data
}

export async function getInventoryTurnover() {
  const { data } = await http.get<ApiResponse<InventoryTurnoverDTO>>('/api/report/inventory-turnover')
  return data
}

