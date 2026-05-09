import http, { unwrap } from '@/api/http'
import type { ApiResult } from '@/types/api'

export interface BorrowRequest {
  userId: number
  biblioId: number
  preferredLibraryId: number
}

export interface BorrowResult {
  status: string
  strategy?: string
  message?: string
  borrowId?: number
  reservationId?: number
  transferId?: number
  copyId?: number
  sourceLibraryId?: number
  targetLibraryId?: number
  taskId?: string
  estimatedArrivalMinutes?: number
  recommendSeatLibraryId?: number
  recommendSeatDate?: string
  recommendSeatReason?: string
  actionRecommendSeat?: boolean
}

export function postBorrowRequest(body: BorrowRequest) {
  return unwrap(http.post<ApiResult<BorrowResult>>('/api/borrow/request', body))
}

export interface PickupConfirmRequest {
  borrowId: number
  userId: number
}

export interface PickupConfirmResponse {
  borrowId: number
  bookTitle?: string
  borrowDate?: string
  dueDate?: string
  libraryName?: string
  recommendSeatLibraryId?: number
  recommendSeatDate?: string
  recommendSeatReason?: string
  actionRecommendSeat?: boolean
}

export function postConfirmPickup(body: PickupConfirmRequest) {
  return unwrap(http.post<ApiResult<PickupConfirmResponse>>('/api/borrow/confirm-pickup', body))
}
