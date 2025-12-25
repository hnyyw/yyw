import axios from 'axios'
import { useUserStore } from '@/stores/user'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  requestId: string
}

export const http = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
})

http.interceptors.request.use((config) => {
  const user = useUserStore()
  if (user.accessToken) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${user.accessToken}`
  }
  return config
})

http.interceptors.response.use(
  (resp) => {
    const body = resp.data as ApiResponse<any>
    if (body && typeof body.code === 'number' && body.code !== 0) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return resp
  },
  (err) => Promise.reject(err),
)

