import http, { unwrap } from '@/api/http'
import type { ApiResult } from '@/types/api'

export function postTransferComplete(transferId: number) {
  return unwrap(http.post<ApiResult<unknown>>('/api/transfer/complete', { transferId }))
}

export function postTransferCompleteBatch(orderId: number) {
  return unwrap(http.post<ApiResult<unknown>>('/api/transfer/complete-batch', { orderId }))
}

export function getTransferProgress(transferId: number) {
  return unwrap(http.get<ApiResult<unknown>>(`/api/transfer/progress/${transferId}`))
}

export function getBatchTransferProgress(orderId: number) {
  return unwrap(http.get<ApiResult<unknown>>(`/api/transfer/progress/batch/${orderId}`))
}

export function getMyTransfers(params: {
  userId: number
  status?: string
  page?: number
  size?: number
}) {
  return unwrap(
    http.get<ApiResult<Record<string, unknown>>>('/api/transfer/my-transfers', { params }),
  )
}

export function getAllTransfers(params: { status?: string; page?: number; size?: number }) {
  return unwrap(http.get<ApiResult<Record<string, unknown>>>('/api/transfer/list', { params }))
}

export function getSuggestionList() {
  return unwrap(http.get<ApiResult<unknown[]>>('/api/transfer/suggestion/list'))
}

export function getSuggestionDetail(id: number) {
  return unwrap(http.get<ApiResult<unknown>>(`/api/transfer/suggestion/${id}`))
}

export function postApproveSuggestion(id: number, approverId: number) {
  return unwrap(
    http.post<ApiResult<null>>(`/api/transfer/suggestion/approve/${id}`, {}, {
      params: { approverId },
    }),
  )
}

export function postRejectSuggestion(id: number, approverId: number, reason?: string) {
  return unwrap(
    http.post<ApiResult<null>>(`/api/transfer/suggestion/reject/${id}`, {}, {
      params: { approverId, reason },
    }),
  )
}

export function postTriggerBalance() {
  return unwrap(http.post<ApiResult<null>>('/api/transfer/suggestion/trigger-balance'))
}
