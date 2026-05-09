import http, { unwrap } from '@/api/http'
import type { ApiResult } from '@/types/api'

export interface ReturnRequest {
  copyId: number
  userId: number
  returnLibraryId: number
}

export interface ReturnResult {
  success?: boolean
  message?: string
  strategy?: string
  [key: string]: unknown
}

export function postReturn(body: ReturnRequest) {
  return unwrap(http.post<ApiResult<ReturnResult>>('/api/book/return', body))
}
