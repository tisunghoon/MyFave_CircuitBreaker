import axios from 'axios'
import type { AxiosError, InternalAxiosRequestConfig } from 'axios'

import { useAuthStore } from '@/features/auth/store'

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
})

// zustand persist가 'myfave-auth' 키에 JSON으로 저장하는 구조에서 토큰 읽기
const getAuthTokens = (): { accessToken: string | null; refreshToken: string | null } => {
  try {
    const stored = localStorage.getItem('myfave-auth')
    if (stored) {
      const parsed = JSON.parse(stored) as { state?: { accessToken?: string; refreshToken?: string } }
      return {
        accessToken: parsed.state?.accessToken ?? null,
        refreshToken: parsed.state?.refreshToken ?? null,
      }
    }
  } catch {
    // localStorage 파싱 실패는 무시하고 비로그인 상태로 처리
  }
  return { accessToken: null, refreshToken: null }
}

// 요청 인터셉터: JWT 자동 주입
apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const { accessToken } = getAuthTokens()
  if (accessToken && config.headers) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

// 응답 인터셉터: 401 발생 시 토큰 재발급
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    // auth 엔드포인트(로그인·재발급)는 재시도 없이 즉시 거부
    const isAuthEndpoint = originalRequest.url?.includes('/auth/')
    if (error.response?.status === 401 && !originalRequest._retry && !isAuthEndpoint) {
      originalRequest._retry = true
      const { refreshToken } = getAuthTokens()

      if (refreshToken) {
        try {
          const { data } = await axios.post(
            `${import.meta.env.VITE_API_BASE_URL}/auth/reissue`,
            { refreshToken },
          )
          // 갱신된 토큰을 zustand store에 반영
          useAuthStore.getState().updateTokens(data.data.accessToken, data.data.refreshToken)
          originalRequest.headers.Authorization = `Bearer ${data.data.accessToken}`
          return apiClient(originalRequest)
        } catch {
          localStorage.clear()
          window.location.href = '/login'
        }
      }
    }
    return Promise.reject(error)
  },
)
