import http, { unwrap } from '@/api/http'
import type { ApiResult } from '@/types/api'

export function triggerReservationExpire() {
  return unwrap(http.post<ApiResult<string>>('/api/task/trigger/reservation-expire'))
}

export function triggerTransferDelayCheck() {
  return unwrap(http.post<ApiResult<string>>('/api/task/trigger/transfer-delay-check'))
}

export function triggerInventoryBalance() {
  return unwrap(http.post<ApiResult<string>>('/api/task/trigger/inventory-balance'))
}

export function triggerReservationWarning() {
  return unwrap(http.post<ApiResult<string>>('/api/task/trigger/reservation-warning'))
}

export function triggerSeatTempLeave() {
  return unwrap(http.post<ApiResult<string>>('/api/task/trigger/seat-temp-leave'))
}
