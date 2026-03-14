import { http, type ApiResponse } from './http'

export type PurchaseStatus = 'CREATED' | 'RECEIVING' | 'DONE'

export type PurchaseOrder = {
  id: number
  poNo: string
  supplierId?: number
  status: PurchaseStatus
  expectedDate?: string
  remark?: string
  createdAt: string
  updatedAt: string
}

export async function listPurchaseOrders(status?: PurchaseStatus | '') {
  const { data } = await http.get<ApiResponse<PurchaseOrder[]>>('/api/purchase/orders', {
    params: status ? { status } : undefined,
  })
  return data
}

export type PurchaseOrderDetailDTO = {
  materialId: number
  qty: number
  price?: number
  remark?: string
}

export type CreatePurchaseOrderRequest = {
  poNo: string
  supplierId?: number
  expectedDate?: string
  remark?: string
  lines: PurchaseOrderDetailDTO[]
}

export async function createPurchaseOrder(req: CreatePurchaseOrderRequest) {
  const { data } = await http.post<ApiResponse<PurchaseOrder>>('/api/purchase/orders', req)
  return data
}

export async function receiveAll(id: number) {
  const { data } = await http.post<ApiResponse<void>>(`/api/purchase/orders/${id}/receive-all`)
  return data
}

