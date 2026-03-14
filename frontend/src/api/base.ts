import { http, type ApiResponse } from './http'

export type Material = {
  id: number
  materialCode: string
  materialName: string
  materialSpec?: string
  unit?: string
  materialType: string
  safetyStock: number
  enabled: boolean
}

export type MaterialCreateRequest = {
  materialCode: string
  materialName: string
  materialSpec?: string
  unit?: string
  materialType: string
  safetyStock: number
}

export type MaterialUpdateRequest = Omit<MaterialCreateRequest, 'materialCode'> & {
  enabled: boolean
}

export type Customer = {
  id: number
  customerName: string
  contactName?: string
  contactPhone?: string
  address?: string
}

export type CustomerCreateRequest = {
  customerName: string
  contactName?: string
  contactPhone?: string
  address?: string
}

export type CustomerUpdateRequest = CustomerCreateRequest

export type Supplier = {
  id: number
  supplierName: string
  contactName?: string
  contactPhone?: string
  address?: string
  enabled: boolean
}

export type SupplierCreateRequest = {
  supplierName: string
  contactName?: string
  contactPhone?: string
  address?: string
}

export type SupplierUpdateRequest = SupplierCreateRequest & {
  enabled: boolean
}

export type BomLine = {
  id: number
  productMaterialId: number
  materialId: number
  qty: number
  remark?: string
}

export type BomLineRequest = {
  materialId: number
  qty: number
  remark?: string
}

export type BomBulkSetRequest = {
  productMaterialId: number
  lines: BomLineRequest[]
}

export async function listMaterials(params?: { materialType?: string; keyword?: string }) {
  const { data } = await http.get<ApiResponse<Material[]>>('/api/base/materials', { params })
  return data
}

export async function createMaterial(req: MaterialCreateRequest) {
  const { data } = await http.post<ApiResponse<Material>>('/api/base/materials', req)
  return data
}

export async function updateMaterial(id: number, req: MaterialUpdateRequest) {
  const { data } = await http.put<ApiResponse<Material>>(`/api/base/materials/${id}`, req)
  return data
}

export async function deleteMaterial(id: number) {
  const { data } = await http.delete<ApiResponse<void>>(`/api/base/materials/${id}`)
  return data
}

export async function listCustomers(keyword?: string) {
  const { data } = await http.get<ApiResponse<Customer[]>>('/api/base/customers', {
    params: keyword ? { keyword } : undefined,
  })
  return data
}

export async function createCustomer(req: CustomerCreateRequest) {
  const { data } = await http.post<ApiResponse<Customer>>('/api/base/customers', req)
  return data
}

export async function updateCustomer(id: number, req: CustomerUpdateRequest) {
  const { data } = await http.put<ApiResponse<Customer>>(`/api/base/customers/${id}`, req)
  return data
}

export async function deleteCustomer(id: number) {
  const { data } = await http.delete<ApiResponse<void>>(`/api/base/customers/${id}`)
  return data
}

export async function listSuppliers(keyword?: string) {
  const { data } = await http.get<ApiResponse<Supplier[]>>('/api/base/suppliers', {
    params: keyword ? { keyword } : undefined,
  })
  return data
}

export async function createSupplier(req: SupplierCreateRequest) {
  const { data } = await http.post<ApiResponse<Supplier>>('/api/base/suppliers', req)
  return data
}

export async function updateSupplier(id: number, req: SupplierUpdateRequest) {
  const { data } = await http.put<ApiResponse<Supplier>>(`/api/base/suppliers/${id}`, req)
  return data
}

export async function deleteSupplier(id: number) {
  const { data } = await http.delete<ApiResponse<void>>(`/api/base/suppliers/${id}`)
  return data
}

export async function listBom(productMaterialId: number) {
  const { data } = await http.get<ApiResponse<BomLine[]>>('/api/base/boms', { params: { productMaterialId } })
  return data
}

export async function bulkSetBom(req: BomBulkSetRequest) {
  const { data } = await http.post<ApiResponse<void>>('/api/base/boms/bulk-set', req)
  return data
}
