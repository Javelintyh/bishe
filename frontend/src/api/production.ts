import { http, type ApiResponse } from './http'

export type WorkOrderStatus = 'TO_PRODUCE' | 'PRODUCING' | 'DONE' | 'STORED'

export type ProductionWorkOrder = {
  id: number
  workOrderNo: string
  orderId: number
  productMaterialId: number
  qty: number
  status: WorkOrderStatus
  dueDate?: string
  assigneeUserId?: number
}

export type ProductionReport = {
  id: number
  workOrderId: number
  processName?: string
  goodQty: number
  badQty: number
  badReasonCode?: string
  badReasonText?: string
  reporterUserId: number
  reportTime: string
}

export type CreateReportRequest = {
  workOrderNo: string
  processName?: string
  goodQty: number
  badQty?: number
  badReasonCode?: string
  badReasonText?: string
}

export type KitCheckItem = {
  materialId: number
  requiredQty: number
  stockQty: number
  shortageQty: number
}

export async function listWorkOrders(params?: { status?: WorkOrderStatus | '' }) {
  const { data } = await http.get<ApiResponse<ProductionWorkOrder[]>>('/api/work-orders', { params })
  return data
}

export async function listReports(workOrderNo?: string) {
  const { data } = await http.get<ApiResponse<ProductionReport[]>>('/api/reports', {
    params: workOrderNo ? { workOrderNo } : undefined,
  })
  return data
}

export async function createReport(req: CreateReportRequest) {
  const { data } = await http.post<ApiResponse<ProductionReport>>('/api/reports', req)
  return data
}

export async function getKitCheck(id: number) {
  const { data } = await http.get<ApiResponse<KitCheckItem[]>>(`/api/work-orders/${id}/kit-check`)
  return data
}

export async function updateWorkOrderStatus(id: number, status: WorkOrderStatus) {
  const { data } = await http.put<ApiResponse<ProductionWorkOrder>>(`/api/work-orders/${id}/status`, null, {
    params: { status },
  })
  return data
}

export async function updateWorkOrderForProduce(id: number, status: WorkOrderStatus, qty: number) {
  const { data } = await http.put<ApiResponse<ProductionWorkOrder>>(`/api/work-orders/${id}/produce`, null, {
    params: { status, qty },
  })
  return data
}
