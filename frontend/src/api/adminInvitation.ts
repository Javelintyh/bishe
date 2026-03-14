import { http, type ApiResponse } from './http'

export type AdminInvitationInfo = {
  code: string
  used: boolean
}

export async function getCurrentInvitation() {
  const { data } = await http.get<ApiResponse<AdminInvitationInfo | null>>('/api/sys/admin-invitation/current')
  return data
}

export async function updateInvitation(code: string) {
  const { data } = await http.post<ApiResponse<AdminInvitationInfo>>('/api/sys/admin-invitation/update', { code })
  return data
}

