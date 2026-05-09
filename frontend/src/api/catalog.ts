import http, { unwrap } from '@/api/http'
import type { ApiResult } from '@/types/api'

export interface LibraryItem {
  id: number
  name: string
}

export interface BiblioItem {
  id: number
  title?: string
  author?: string
  isbn?: string
}

export interface ActiveLoanRow {
  borrowId: number
  userId: number
  copyId: number
  biblioId?: number
  bookTitle?: string
  status?: string
  dueTime?: string
  suggestedReturnLibraryId?: number
  suggestedReturnLibraryName?: string
}

export interface PickupPendingRow {
  borrowId: number
  userId: number
  copyId: number
  bookTitle?: string
  status?: string
  pickupLibraryId?: number
  pickupLibraryName?: string
  pickupDeadline?: string
}

export interface SeatAreaItem {
  id: number
  libraryId: number
  name?: string
  floor?: number
}

export interface SeatItem {
  id: number
  areaId: number
  seatNo?: string
  hasPower?: number
  status?: string
}

export function getLibraries() {
  return unwrap(http.get<ApiResult<LibraryItem[]>>('/api/catalog/libraries'))
}

export function getBiblios(keyword?: string, limit = 30) {
  return unwrap(
    http.get<ApiResult<BiblioItem[]>>('/api/catalog/biblios', {
      params: { keyword: keyword || undefined, limit },
    }),
  )
}

export function getActiveLoans(userId: number) {
  return unwrap(http.get<ApiResult<ActiveLoanRow[]>>('/api/catalog/borrows/active', { params: { userId } }))
}

export function getPickupPending(userId: number) {
  return unwrap(http.get<ApiResult<PickupPendingRow[]>>('/api/catalog/borrows/pickup-pending', { params: { userId } }))
}

export interface BorrowHistoryRow {
  borrowId: number
  userId: number
  copyId: number
  biblioId?: number
  bookTitle?: string
  status?: string
  borrowTime?: string
  dueTime?: string
  returnTime?: string
  actualPickupTime?: string
  pickupLibraryId?: number
  pickupLibraryName?: string
}

export function getBorrowHistory(params: { userId: number; status?: string; page?: number; size?: number }) {
  return unwrap(http.get<ApiResult<Record<string, unknown>>>('/api/catalog/borrows/history', { params }))
}

export function getSeatAreas(libraryId: number) {
  return unwrap(http.get<ApiResult<SeatAreaItem[]>>('/api/catalog/seat-areas', { params: { libraryId } }))
}

export function getSeats(areaId: number, limit = 200) {
  return unwrap(http.get<ApiResult<SeatItem[]>>('/api/catalog/seats', { params: { areaId, limit } }))
}

export function getEquipmentPage(params: { libraryId?: number; page?: number; size?: number }) {
  return unwrap(http.get<ApiResult<Record<string, unknown>>>('/api/catalog/equipment', { params }))
}

/** 管理端调拨行（与后端 AdminTransferRowDTO 对齐） */
export interface AdminTransferRow {
  transferId: number
  copyId?: number
  biblioId?: number
  bookTitle?: string
  fromLibraryId?: number
  fromLibraryName?: string
  toLibraryId?: number
  toLibraryName?: string
  status?: string
  orderId?: number | null
  suggestionId?: number | null
  transferReason?: string
  receiverUserId?: number | null
  requestTime?: string
  completeTime?: string
  estimatedArrivalTime?: string
  actualArrivalTime?: string
}

export function getAdminTransfersPage(params: { status?: string; page?: number; size?: number }) {
  return unwrap(http.get<ApiResult<Record<string, unknown>>>('/api/catalog/transfers', { params }))
}

/** 管理端工单行（与后端 AdminFaultTicketRowDTO 对齐） */
export interface AdminFaultTicketRow {
  id: number
  libraryId?: number
  libraryName?: string
  areaId?: number | null
  seatId?: number | null
  equipmentId?: number | null
  faultType?: string
  severity?: string
  status?: string
  title?: string
  description?: string
  adminRemark?: string
  reportSource?: string
  reportUserId?: number | null
  assignee?: string
  createdTime?: string
  updatedTime?: string
  resolvedTime?: string
}

export function getAdminFaultTicketsPage(params: Record<string, string | number | undefined>) {
  return unwrap(http.get<ApiResult<Record<string, unknown>>>('/api/catalog/fault-tickets', { params }))
}
