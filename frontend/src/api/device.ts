import { http, type ApiResponse } from './http'

export type DeviceAsset = {
  id: number
  deviceCode: string
  deviceName: string
  model?: string
  location?: string
  status: string
  remark?: string
  createdAt: string
  updatedAt: string
}

export type DeviceStatusLog = {
  id: number
  deviceId: number
  status: string
  remark?: string
  operatorUserId?: number
  createdAt: string
}

export type DeviceAssetRequest = {
  deviceCode: string
  deviceName: string
  model?: string
  location?: string
  status?: string
  remark?: string
}

export type DeviceStatusChangeRequest = {
  status: string
  remark?: string
}

export async function listDevices() {
  const { data } = await http.get<ApiResponse<DeviceAsset[]>>('/api/devices')
  return data
}

export async function createDevice(req: DeviceAssetRequest) {
  const { data } = await http.post<ApiResponse<DeviceAsset>>('/api/devices', req)
  return data
}

export async function updateDevice(id: number, req: DeviceAssetRequest) {
  const { data } = await http.put<ApiResponse<DeviceAsset>>(`/api/devices/${id}`, req)
  return data
}

export async function deleteDevice(id: number) {
  const { data } = await http.delete<ApiResponse<void>>(`/api/devices/${id}`)
  return data
}

export async function changeDeviceStatus(id: number, req: DeviceStatusChangeRequest) {
  const { data } = await http.post<ApiResponse<DeviceAsset>>(`/api/devices/${id}/status`, req)
  return data
}

export async function listDeviceLogs(id: number) {
  const { data } = await http.get<ApiResponse<DeviceStatusLog[]>>(`/api/devices/${id}/logs`)
  return data
}

