/** 后端统一包装（多数 /api 接口） */
export interface ApiResult<T> {
  code: number
  message: string
  data: T
}
