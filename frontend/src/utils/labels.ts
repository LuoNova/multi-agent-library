import type { BorrowResult, PickupConfirmResponse } from '@/api/borrow'

/** 空值展示 */
export function pickStr(v: unknown): string {
  if (v === null || v === undefined || v === '') return '—'
  if (typeof v === 'boolean') return v ? '是' : '否'
  if (typeof v === 'object') return JSON.stringify(v)
  return String(v)
}

export function borrowStrategyZh(s?: string): string {
  const m: Record<string, string> = {
    LOCAL_LOAN: '本馆借阅',
    TRANSFER_PROVIDE: '跨馆调拨',
    RESERVATION: '预约排队',
  }
  return (s && m[s]) || s || '—'
}

export function recommendReasonZh(s?: string): string {
  const m: Record<string, string> = {
    LOCAL_LOAN: '本馆借阅推荐',
    TRANSFER_PICKUP: '调拨取书后推荐',
    RESERVATION_PICKUP: '预约取书后推荐',
  }
  return (s && m[s]) || s || '—'
}

export function borrowResultRows(r: BorrowResult): { label: string; value: string }[] {
  const rows: { label: string; value: string }[] = []
  rows.push({
    label: '处理结果',
    value: r.status === 'SUCCESS' ? '成功' : r.status === 'FAILED' ? '未成功' : pickStr(r.status),
  })
  if (r.message) rows.push({ label: '说明', value: r.message })
  if (r.strategy) rows.push({ label: '业务策略', value: borrowStrategyZh(r.strategy) })
  if (r.borrowId != null) rows.push({ label: '借阅记录编号', value: pickStr(r.borrowId) })
  if (r.reservationId != null) rows.push({ label: '预约记录编号', value: pickStr(r.reservationId) })
  if (r.transferId != null) rows.push({ label: '调拨记录编号', value: pickStr(r.transferId) })
  if (r.copyId != null) rows.push({ label: '图书副本编号', value: pickStr(r.copyId) })
  if (r.sourceLibraryId != null) rows.push({ label: '调拨来源馆编号', value: pickStr(r.sourceLibraryId) })
  if (r.targetLibraryId != null) rows.push({ label: '目标馆编号', value: pickStr(r.targetLibraryId) })
  if (r.taskId) rows.push({ label: '任务编号', value: r.taskId })
  if (r.estimatedArrivalMinutes != null)
    rows.push({ label: '预计到达（分钟）', value: pickStr(r.estimatedArrivalMinutes) })
  if (r.recommendSeatLibraryId != null)
    rows.push({ label: '推荐预约座位所在馆', value: pickStr(r.recommendSeatLibraryId) })
  if (r.recommendSeatDate) rows.push({ label: '推荐预约日期', value: r.recommendSeatDate })
  if (r.recommendSeatReason) rows.push({ label: '推荐原因', value: recommendReasonZh(r.recommendSeatReason) })
  if (r.actionRecommendSeat != null)
    rows.push({ label: '是否建议预约座位', value: r.actionRecommendSeat ? '是' : '否' })
  return rows
}

export function pickupResultRows(r: PickupConfirmResponse): { label: string; value: string }[] {
  const rows: { label: string; value: string }[] = []
  if (r.borrowId != null) rows.push({ label: '借阅记录编号', value: pickStr(r.borrowId) })
  if (r.bookTitle) rows.push({ label: '图书名称', value: r.bookTitle })
  if (r.borrowDate) rows.push({ label: '借阅时间', value: pickStr(r.borrowDate) })
  if (r.dueDate) rows.push({ label: '应还时间', value: pickStr(r.dueDate) })
  if (r.libraryName) rows.push({ label: '取书馆', value: r.libraryName })
  if (r.recommendSeatLibraryId != null)
    rows.push({ label: '推荐占座馆编号', value: pickStr(r.recommendSeatLibraryId) })
  if (r.recommendSeatDate) rows.push({ label: '推荐预约日期', value: r.recommendSeatDate })
  if (r.recommendSeatReason) rows.push({ label: '推荐原因', value: recommendReasonZh(r.recommendSeatReason) })
  if (r.actionRecommendSeat != null)
    rows.push({ label: '是否建议预约座位', value: r.actionRecommendSeat ? '是' : '否' })
  return rows
}

export function returnResultRows(r: Record<string, unknown>): { label: string; value: string }[] {
  const rows: { label: string; value: string }[] = []
  if ('success' in r) rows.push({ label: '是否成功', value: r.success ? '是' : '否' })
  if (r.status) rows.push({ label: '状态', value: r.status === 'SUCCESS' ? '成功' : r.status === 'FAILED' ? '失败' : pickStr(r.status) })
  if (r.message) rows.push({ label: '说明', value: String(r.message) })
  if ('hasReservation' in r) rows.push({ label: '是否涉及预约', value: pickStr(r.hasReservation) })
  if (r.reservatorId != null) rows.push({ label: '预约用户编号', value: pickStr(r.reservatorId) })
  if ('localReservation' in r) rows.push({ label: '是否就地预留', value: pickStr(r.localReservation) })
  if (r.reservationId != null) rows.push({ label: '预约记录编号', value: pickStr(r.reservationId) })
  if (r.reserveExpireTime) rows.push({ label: '预留过期时间', value: pickStr(r.reserveExpireTime) })
  if (r.transferId != null) rows.push({ label: '调拨记录编号', value: pickStr(r.transferId) })
  return rows
}

export function faultTypeZh(s?: string): string {
  const m: Record<string, string> = {
    seat_broken: '座椅损坏',
    power_failure: '电源故障',
    env_issue: '环境问题',
    network_fault: '网络故障',
    other: '其它',
  }
  return (s && m[s]) || s || '—'
}

export function severityZh(s?: string): string {
  const m: Record<string, string> = { low: '低', medium: '中', high: '高' }
  return (s && m[s]) || s || '—'
}

export function faultStatusZh(s?: string): string {
  const m: Record<string, string> = {
    REPORTED: '已报修',
    ACCEPTED: '已受理',
    IN_PROGRESS: '处理中',
    RESTORED: '已恢复',
    CLOSED: '已关闭',
  }
  return (s && m[s]) || s || '—'
}

export function reportSourceZh(s?: string): string {
  const m: Record<string, string> = {
    USER: '用户',
    MONITOR: '监控',
    ADMIN: '管理员',
    SYSTEM: '系统',
  }
  return (s && m[s]) || s || '—'
}

export function resourceTypeZh(s?: string): string {
  const m: Record<string, string> = {
    LIBRARY: '图书馆',
    SEAT_AREA: '座位区域',
    SEAT: '座位',
    EQUIPMENT: '设备',
  }
  return (s && m[s]) || s || '—'
}

export function equipmentStatusZh(s?: string): string {
  const m: Record<string, string> = {
    NORMAL: '正常',
    FAULT: '故障',
    MAINTAIN: '维护',
    DISABLED: '停用',
  }
  return (s && m[s]) || s || '—'
}

/** 列表类 data 摘要 */
export function pagedDataSummary(data: unknown): { label: string; value: string }[] {
  if (!data || typeof data !== 'object') return []
  const d = data as Record<string, unknown>
  const rows: { label: string; value: string }[] = []
  if ('total' in d) rows.push({ label: '总条数', value: pickStr(d.total) })
  if ('page' in d) rows.push({ label: '当前页', value: pickStr(d.page) })
  if ('size' in d) rows.push({ label: '每页条数', value: pickStr(d.size) })
  if (Array.isArray(d.records)) rows.push({ label: '本页记录数', value: String(d.records.length) })
  return rows
}

/** 调拨进度、单条结果等：常见扁平字段 */
export function transferLikeRows(data: unknown): { label: string; value: string }[] {
  if (!data || typeof data !== 'object') return []
  const d = data as Record<string, unknown>
  const map: Record<string, string> = {
    transferId: '调拨记录编号',
    orderId: '调拨单编号',
    status: '状态',
    message: '说明',
    progressPercentage: '进度（%）',
    biblioTitle: '图书名称',
    fromLibraryName: '源馆',
    toLibraryName: '目标馆',
    estimatedArrivalTime: '预计到达时间',
    requestTime: '申请时间',
    completeTime: '完成时间',
  }
  const rows: { label: string; value: string }[] = []
  for (const [k, v] of Object.entries(d)) {
    if (v === null || v === undefined) continue
    if (typeof v === 'object' && !Array.isArray(v)) continue
    if (Array.isArray(v)) continue
    rows.push({ label: map[k] || k, value: pickStr(v) })
  }
  return rows.slice(0, 24)
}

export function equipmentEntityRows(data: unknown): { label: string; value: string }[] {
  if (!data || typeof data !== 'object') return []
  const e = data as Record<string, unknown>
  const rows: { label: string; value: string }[] = []
  const map: Record<string, string> = {
    id: '设备编号',
    libraryId: '所在馆编号',
    areaId: '区域编号',
    type: '设备类型',
    name: '名称',
    location: '位置',
    status: '状态',
    remark: '备注',
    installTime: '安装时间',
    createTime: '创建时间',
    updateTime: '更新时间',
  }
  for (const [k, v] of Object.entries(e)) {
    if (v === null || v === undefined) continue
    const label = map[k] || k
    const val = k === 'status' ? equipmentStatusZh(String(v)) : pickStr(v)
    rows.push({ label, value: val })
  }
  return rows
}

/** 调拨列表优先展示分页摘要，否则展示扁平字段 */
export function transferResponseRows(data: unknown): { label: string; value: string }[] {
  const page = pagedDataSummary(data)
  if (page.length) return page
  return transferLikeRows(data)
}

export function faultReportRows(vo: Record<string, unknown>): { label: string; value: string }[] {
  const rows: { label: string; value: string }[] = []
  const map: Record<string, string> = {
    id: '工单编号',
    libraryId: '图书馆编号',
    areaId: '区域编号',
    seatId: '座位编号',
    equipmentId: '设备编号',
    faultType: '故障类型',
    severity: '严重程度',
    status: '状态',
    title: '标题',
    description: '描述',
    adminRemark: '运维备注',
    reportSource: '来源',
    reportUserId: '报修用户编号',
    assignee: '负责人',
    createdTime: '创建时间',
    updatedTime: '更新时间',
    resolvedTime: '解决/关闭时间',
  }
  for (const [k, v] of Object.entries(vo)) {
    if (v === null || v === undefined) continue
    let val = pickStr(v)
    if (k === 'faultType') val = faultTypeZh(String(v))
    else if (k === 'severity') val = severityZh(String(v))
    else if (k === 'status') val = faultStatusZh(String(v))
    else if (k === 'reportSource') val = reportSourceZh(String(v))
    rows.push({ label: map[k] || k, value: val })
  }
  return rows
}

export function healthResultRows(data: unknown): { label: string; value: string }[] {
  if (!data || typeof data !== 'object') return []
  const d = data as Record<string, unknown>
  const results = d.results
  if (!Array.isArray(results)) return [{ label: '说明', value: '无 results 数组' }]
  return [
    { label: '查询资源条数', value: String(results.length) },
    {
      label: '不可用资源数',
      value: String(results.filter((x: unknown) => x && typeof x === 'object' && (x as { available?: boolean }).available === false).length),
    },
  ]
}

export function seatAvailabilityRows(data: unknown): { label: string; value: string }[] {
  if (!data || typeof data !== 'object') return []
  const d = data as Record<string, unknown>
  const rows: { label: string; value: string }[] = []
  if (d.availableTotal != null) rows.push({ label: '可用座位总数（自动分配模式）', value: pickStr(d.availableTotal) })
  if (d.total != null) rows.push({ label: '可用座位数（列表模式）', value: pickStr(d.total) })
  if (d.assignedSeat && typeof d.assignedSeat === 'object') {
    const a = d.assignedSeat as Record<string, unknown>
    rows.push({ label: '推荐座位', value: `${pickStr(a.areaName)} / ${pickStr(a.seatNo)}（编号 ${pickStr(a.seatId)}）` })
  }
  return rows
}
