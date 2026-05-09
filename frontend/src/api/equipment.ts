import http, { unwrap } from '@/api/http'
import type { ApiResult } from '@/types/api'

export function patchEquipmentStatus(id: number, status: string) {
  return unwrap(http.patch<ApiResult<unknown>>(`/api/equipment/${id}/status`, { status }))
}
