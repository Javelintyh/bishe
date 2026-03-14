import { http, type ApiResponse } from './http'

export type QualityReason = {
  id: number
  reasonCode: string
  reasonName: string
  enabled: boolean
  sortNo: number
}

export async function listQualityReasons(enabledOnly = true) {
  const { data } = await http.get<ApiResponse<QualityReason[]>>('/api/quality/reasons', {
    params: enabledOnly ? { enabledOnly: true } : undefined,
  })
  return data
}

