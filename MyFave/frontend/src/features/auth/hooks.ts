import { useMutation } from '@tanstack/react-query'

import { authApi } from './api'
import { PENDING_AVATAR_KEY } from './storageKeys'
import { useAuthStore } from './store'
import type {
  FindIdRequest,
  ResetPasswordRequest,
  SignUpSendCodeRequest,
  SignUpVerifyCodeRequest,
  SignUpRequest,
} from './types'

// 회원가입 시 부여된 곰돌이 아바타 URL 을 다음 로그인까지 임시 보관 (storageKeys.ts 참조).
// 백엔드 LoginResponse 가 profileImageUrl 을 아직 내려주지 않을 때 fallback 으로 사용.
interface PendingAvatar {
  pendingEmail: string
  url: string
}

function readPendingAvatar(email: string): string | undefined {
  if (!email) return undefined
  const raw = localStorage.getItem(PENDING_AVATAR_KEY)
  if (!raw) return undefined
  try {
    const parsed = JSON.parse(raw) as PendingAvatar
    if (parsed.pendingEmail !== email) return undefined
    localStorage.removeItem(PENDING_AVATAR_KEY)
    return parsed.url
  } catch {
    return undefined
  }
}

export function useLogin() {
  const login = useAuthStore((s) => s.login)
  return useMutation({
    mutationFn: authApi.login,
    onSuccess: (data, variables) => {
      const profileImageUrl = data.profileImageUrl ?? readPendingAvatar(variables.email)
      login({
        user: {
          id: data.userId,
          email: variables.email,
          nickname: data.nickname,
          profileImageUrl,
        },
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
      })
    },
  })
}

export function useKakaoLogin() {
  const login = useAuthStore((s) => s.login)
  return useMutation({
    mutationFn: (authorizationCode: string) =>
      authApi.socialLogin('kakao', { authorizationCode }),
    onSuccess: (data) => {
      const profileImageUrl = data.profileImageUrl ?? readPendingAvatar(data.email)
      login({
        user: {
          id: data.userId,
          email: data.email,
          nickname: data.nickname,
          profileImageUrl,
        },
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
      })
    },
  })
}

export function useUser() {
  return useAuthStore((s) => s.user)
}

export function useIsAuthenticated() {
  return useAuthStore((s) => s.accessToken !== null)
}

export function useLogout() {
  return useAuthStore((s) => s.logout)
}

export function useSendSignUpCode() {
  return useMutation({
    mutationFn: (body: SignUpSendCodeRequest) => authApi.sendSignUpCode(body),
  })
}

export function useVerifySignUpCode() {
  return useMutation({
    mutationFn: (body: SignUpVerifyCodeRequest) => authApi.verifySignUpCode(body),
  })
}

export function useSignUp() {
  return useMutation({
    mutationFn: (body: SignUpRequest) => authApi.signUp(body),
  })
}

export function useFindId() {
  return useMutation({
    mutationFn: (body: FindIdRequest) => authApi.findId(body),
  })
}

export function useResetPassword() {
  return useMutation({
    mutationFn: (body: ResetPasswordRequest) => authApi.resetPassword(body),
  })
}
