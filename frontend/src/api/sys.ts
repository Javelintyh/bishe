import { http, type ApiResponse } from './http'

export type RoleCode = 'ADMIN' | 'WORKSHOP' | 'WAREHOUSE'

export type UserVO = {
  id: number
  username: string
  roleCode: RoleCode
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export type CreateUserRequest = {
  username: string
  password?: string
  roleCode: RoleCode
}

export type UpdateUserRequest = {
  roleCode: RoleCode
  enabled?: boolean
}

export type ChangePasswordRequest = {
  oldPassword: string
  newPassword: string
}

export async function listUsers() {
  const { data } = await http.get<ApiResponse<UserVO[]>>('/api/sys/users')
  return data
}

export async function createUser(req: CreateUserRequest) {
  const { data } = await http.post<ApiResponse<UserVO>>('/api/sys/users', req)
  return data
}

export async function updateUser(id: number, req: UpdateUserRequest) {
  const { data } = await http.put<ApiResponse<UserVO>>(`/api/sys/users/${id}`, req)
  return data
}

export async function deleteUser(id: number) {
  const { data } = await http.delete<ApiResponse<void>>(`/api/sys/users/${id}`)
  return data
}

export async function resetPassword(id: number) {
  const { data } = await http.post<ApiResponse<void>>(`/api/sys/users/${id}/reset-password`)
  return data
}

export async function changePassword(req: ChangePasswordRequest) {
  const { data } = await http.post<ApiResponse<void>>('/api/sys/users/change-password', req)
  return data
}

