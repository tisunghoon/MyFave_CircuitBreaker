import { create } from 'zustand'
import { persist } from 'zustand/middleware'

import { queryClient } from '@/app/queryClient'
import { LEGACY_AVATAR_PREFIX, PENDING_AVATAR_KEY } from './storageKeys'
import type { User } from './types'

// 사용자 식별 정보가 담긴 모든 로컬 캐시 키 — 로그인/로그아웃 시 모두 청소.
// pending avatar 단일 키 + 레거시 이메일-포함 prefix(myfave-avatar:{email}) 도 정리해
// 이전 사용자 식별자가 브라우저에 잔존하지 않도록 보장 (CodeRabbit Major).
const USER_STORE_KEYS = ['myfave-shipping', 'myfave-cart', 'myfave-coupons', PENDING_AVATAR_KEY]

const clearUserStores = () => {
  queryClient.clear()
  USER_STORE_KEYS.forEach((key) => localStorage.removeItem(key))
  // 레거시 prefix 키들 일괄 제거 — 단일 키 도입 전에 저장된 잔존 엔트리 청소용.
  for (let i = localStorage.length - 1; i >= 0; i--) {
    const key = localStorage.key(i)
    if (key && key.startsWith(LEGACY_AVATAR_PREFIX)) localStorage.removeItem(key)
  }
}

interface AuthState {
  user: User | null
  accessToken: string | null
  refreshToken: string | null
  login: (params: { user: User; accessToken: string; refreshToken: string }) => void
  logout: () => void
  updateTokens: (accessToken: string, refreshToken: string) => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      login: ({ user, accessToken, refreshToken }) => {
        clearUserStores()
        set({ user, accessToken, refreshToken })
      },
      logout: () => {
        clearUserStores()
        set({ user: null, accessToken: null, refreshToken: null })
      },
      updateTokens: (accessToken, refreshToken) => {
        set({ accessToken, refreshToken })
      },
    }),
    { name: 'myfave-auth' },
  ),
)
