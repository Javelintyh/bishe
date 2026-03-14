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
  const { data } = await http.get<ApiResponse<MessageNotice[]>>('/api/messages', {
    params: unreadOnly ? { unreadOnly: true } : undefined,
  })
  return data
}

export async function markRead(id: number) {
  const { data } = await http.post<ApiResponse<void>>(`/api/messages/${id}/read`)
  return data
}

export async function scanMessages() {
  const { data } = await http.post<ApiResponse<void>>('/api/messages/scan')
  return data
}

