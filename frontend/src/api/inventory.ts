import { http, type ApiResponse } from './http'

export type InventoryStock = {
  id: number
  materialId: number
  qty: number
  updatedAt: string
}

export type InventoryRecord = {
  id: number
  materialId: number
  changeQty: number
  bizType: string
  bizId?: string
  remark?: string
  createdAt: string
}

export type StockChangeRequest = {
  materialId: number
  qty: number
  bizType: string
  bizId?: string
}

export async function listStocks() {
  const { data } = await http.get<ApiResponse<InventoryStock[]>>('/api/inventory/stocks')
  return data
}

export async function listInventoryRecords(params?: { materialId?: number; bizType?: string }) {
  const { data } = await http.get<ApiResponse<InventoryRecord[]>>('/api/inventory/records', { params })
  return data
}

export async function inbound(req: StockChangeRequest) {
  const { data } = await http.post<ApiResponse<void>>('/api/inventory/in', req)
  return data
}

export async function outbound(req: StockChangeRequest) {
  const { data } = await http.post<ApiResponse<void>>('/api/inventory/out', req)
  return data
}
