import { http, type ApiResponse } from './http'

export type MessageNotice = {
  id: number
  noticeType: string
  title: string
  content?: string
  level: string
  relatedType?: string
  relatedId?: string
  read: boolean
  createdAt: string
}

export async function listMessages(unreadOnly = false) {
  const { data } = await http.get<ApiResponse<any[]>>('/api/messages', {
    params: unreadOnly ? { unreadOnly: true } : undefined,
  })
  // 后端字段为 isRead，这里统一映射为 read，便于前端使用
  const mapped: ApiResponse<MessageNotice[]> = {
    ...data,
    data: (data.data ?? []).map((m: any) => ({
      ...m,
      read: m.read ?? m.isRead ?? false,
    })),
  }
  return mapped
}

export async function markRead(id: number) {
  const { data } = await http.post<ApiResponse<void>>(`/api/messages/${id}/read`)
  return data
}

export async function scanMessages() {
  const { data } = await http.post<ApiResponse<void>>('/api/messages/scan')
  return data
}

export type CreatePurchaseRequestPayload = {
  materialId: number
  remark?: string
  qty?: string
}

export async function createPurchaseRequest(payload: CreatePurchaseRequestPayload) {
  const { materialId, remark, qty } = payload
  const params: Record<string, string | number> = { materialId }
  if (remark) params.remark = remark
  if (qty) params.qty = qty
  const { data } = await http.post<ApiResponse<void>>('/api/messages/purchase-requests', undefined, {
    params,
  })
  return data
}

export async function urgeWarehouseInbound(materialId: number, scene: 'RAW' | 'PRODUCT') {
  const { data } = await http.post<ApiResponse<void>>('/api/messages/urge-warehouse-inbound', undefined, {
    params: { materialId, scene },
  })
  return data
}

export async function urgeWorkshopProduce(materialId: number) {
  const { data } = await http.post<ApiResponse<void>>('/api/messages/urge-workshop-produce', undefined, {
    params: { materialId },
  })
  return data
}

