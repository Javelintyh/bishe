import { http, type ApiResponse } from './http'
import type { RoleCode } from '@/utils/auth'

export type LoginRequest = {
  username: string
  password: string
}

export type LoginResponse = {
  token: string
  username: string
  roleCode: RoleCode
  userId?: number | null
}

export async function login(req: LoginRequest) {
  try {
    const { data } = await http.post<ApiResponse<LoginResponse>>('/api/auth/login', req)
    return data
  } catch (e: any) {
    const respData = e?.response?.data as ApiResponse<LoginResponse> | undefined
    if (respData) {
      // 后端错误时也返回统一的 ApiResponse 结构，这里直接返回给上层处理 code/message
      return respData
    }
    throw e
  }
}

export type RegisterRequest = {
  username: string
  password: string
  roleCode: RoleCode
  invitationCode?: string
}

export async function register(req: RegisterRequest) {
  const { data } = await http.post<ApiResponse<void>>('/api/auth/register', req)
  return data
}

