import axios, { type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '@/types/api'

const http = axios.create({
  baseURL: '',
  timeout: 120000,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.response.use(
  (res: AxiosResponse) => {
    const d = res.data
    if (
      d &&
      typeof d === 'object' &&
      'code' in d &&
      typeof (d as ApiResult<unknown>).code === 'number'
    ) {
      const r = d as ApiResult<unknown>
      if (r.code !== 200) {
        ElMessage.error(r.message || '请求失败')
        return Promise.reject(new Error(r.message || '请求失败'))
      }
    }
    return res
  },
  (err) => {
    const msg = err.response?.data?.message || err.message || '网络错误'
    ElMessage.error(msg)
    return Promise.reject(err)
  },
)

/** 解析统一 Result，返回 data */
export async function unwrap<T>(p: Promise<AxiosResponse<ApiResult<T>>>): Promise<T> {
  const { data } = await p
  return data.data
}

export default http
