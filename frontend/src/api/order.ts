import { http, type ApiResponse } from './http'

// 待处理 -> 生产中 -> 已制成 -> 已发货 -> 已完成
export type OrderStatus = 'PENDING' | 'PRODUCING' | 'COMPLETED' | 'SHIPPED' | 'DONE'

export type OrderVO = {
  id: number
  orderNo: string
  customerId: number
  status: OrderStatus
  deliveryDate?: string
  pinned: boolean
  urgent: boolean
  productMaterialId?: number
  qty?: number
  workOrderNo?: string
  createdAt: string
  updatedAt: string
}

export type CreateOrderRequest = {
  orderNo: string
  customerId: number
  productMaterialId: number
  qty: number
  deliveryDate?: string
  urgent?: boolean
  pinned?: boolean
}

export async function listOrders(status?: OrderStatus | '') {
  const { data } = await http.get<ApiResponse<OrderVO[]>>('/api/orders', {
    params: status ? { status } : undefined,
  })
  return data
}

export async function createOrder(req: CreateOrderRequest) {
  const { data } = await http.post<ApiResponse<OrderVO>>('/api/orders', req)
  return data
}

export async function setOrderPinned(id: number, pinned: boolean) {
  const { data } = await http.put<ApiResponse<OrderVO>>(`/api/orders/${id}/pinned`, null, {
    params: { pinned },
  })
  return data
}

export async function updateOrderStatus(id: number, status: OrderStatus) {
  const { data } = await http.put<ApiResponse<OrderVO>>(`/api/orders/${id}/status`, undefined, {
    params: { status },
  })
  return data
}

export async function updateOrder(id: number, req: CreateOrderRequest) {
  const { data } = await http.put<ApiResponse<OrderVO>>(`/api/orders/${id}`, req)
  return data
}

export async function deleteOrder(id: number) {
  const { data } = await http.delete<ApiResponse<void>>(`/api/orders/${id}`)
  return data
}
