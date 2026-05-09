import http, { unwrap } from '@/api/http'
import type { ApiResult } from '@/types/api'

export function getSeatAvailable(params: {
  libraryId: number
  reservationDate: string
  startTime: string
  endTime: string
  areaId?: number
  hasPower?: boolean
  autoAssign?: boolean
}) {
  return unwrap(http.get<ApiResult<unknown>>('/api/seat/available', { params }))
}

export interface SeatReservationCreateRequest {
  userId: number
  seatId: number
  libraryId: number
  reservationDate: string
  startTime: string
  endTime: string
  borrowId?: number
  source?: string
}

export function postSeatReservation(body: SeatReservationCreateRequest) {
  return unwrap(http.post<ApiResult<unknown>>('/api/seat/reservation', body))
}

export function postCancelReservation(reservationId: number, userId: number) {
  return unwrap(
    http.post<ApiResult<null>>('/api/seat/reservation/cancel', null, {
      params: { reservationId, userId },
    }),
  )
}

export function postCheckIn(reservationId: number, userId: number) {
  return unwrap(
    http.post<ApiResult<null>>('/api/seat/reservation/check-in', null, {
      params: { reservationId, userId },
    }),
  )
}

export function postTempLeave(reservationId: number, userId: number) {
  return unwrap(
    http.post<ApiResult<null>>('/api/seat/reservation/temp-leave', null, {
      params: { reservationId, userId },
    }),
  )
}

export function postEndTempLeave(reservationId: number, userId: number) {
  return unwrap(
    http.post<ApiResult<null>>('/api/seat/reservation/temp-leave/end', null, {
      params: { reservationId, userId },
    }),
  )
}

export function postFinishUse(reservationId: number, userId: number) {
  return unwrap(
    http.post<ApiResult<null>>('/api/seat/reservation/finish', null, {
      params: { reservationId, userId },
    }),
  )
}

export function getMyReservations(params: {
  userId: number
  status?: string
  fromDate?: string
  toDate?: string
  page?: number
  size?: number
}) {
  return unwrap(http.get<ApiResult<Record<string, unknown>>>('/api/seat/reservation/my', { params }))
}
