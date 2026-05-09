import http, { unwrap } from '@/api/http'
import type { ApiResult } from '@/types/api'

export interface FaultReportCreateRequest {
  libraryId?: number | null
  areaId?: number | null
  seatId?: number | null
  equipmentId?: number | null
  faultType: string
  severity: string
  title: string
  description?: string
  reportSource: string
  reportUserId?: number | null
}

export interface FaultReportVO {
  id: number
  libraryId?: number
  areaId?: number
  seatId?: number
  equipmentId?: number
  faultType: string
  severity: string
  status: string
  title: string
  description?: string
  adminRemark?: string
  reportSource: string
  reportUserId?: number
  assignee?: string
  createdTime?: string
  updatedTime?: string
  resolvedTime?: string
}

export function postFaultReport(body: FaultReportCreateRequest) {
  return unwrap(http.post<ApiResult<FaultReportVO>>('/api/fault/report', body))
}

export function getFaultList(params: Record<string, string | number | undefined>) {
  return unwrap(http.get<ApiResult<Record<string, unknown>>>('/api/fault/list', { params }))
}

export function getFaultDetail(id: number) {
  return unwrap(http.get<ApiResult<FaultReportVO>>(`/api/fault/${id}`))
}

export interface FaultStatusPatchRequest {
  status: string
  assignee?: string
  adminRemark?: string
}

export function patchFaultStatus(id: number, body: FaultStatusPatchRequest) {
  return unwrap(http.patch<ApiResult<FaultReportVO>>(`/api/fault/${id}/status`, body))
}

export interface FaultHealthResourceRef {
  resourceType: string
  resourceId: number
}

export function postFaultHealthQuery(resources: FaultHealthResourceRef[]) {
  return unwrap(http.post<ApiResult<unknown>>('/api/fault/health/query', { resources }))
}
