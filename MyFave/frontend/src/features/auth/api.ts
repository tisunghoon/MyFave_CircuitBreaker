import { apiClient } from '@/shared/api/axios'
import type { ApiResponse } from '@/shared/api/types'

import type {
  FindIdRequest,
  FindIdResponse,
  LoginRequest,
  LoginResponse,
  ResetPasswordRequest,
  SignUpSendCodeRequest,
  SignUpVerifyCodeRequest,
  SignUpVerifyCodeResponse,
  SignUpRequest,
  SignUpResponse,
  SocialLoginRequest,
  SocialLoginResponse,
} from './types'

export const authApi = {
  sendSignUpCode: async (body: SignUpSendCodeRequest): Promise<void> => {
    await apiClient.post<ApiResponse<void>>('/auth/signup/send-code', body)
  },

  verifySignUpCode: async (body: SignUpVerifyCodeRequest): Promise<SignUpVerifyCodeResponse> => {
    const { data } = await apiClient.post<ApiResponse<SignUpVerifyCodeResponse>>(
      '/auth/signup/verify-code',
      body,
    )
    return data.data
  },

  signUp: async (body: SignUpRequest): Promise<SignUpResponse> => {
    const { data } = await apiClient.post<ApiResponse<SignUpResponse>>('/auth/signup', body)
    return data.data
  },

  login: async (body: LoginRequest) => {
    const { data } = await apiClient.post<ApiResponse<LoginResponse>>('/auth/login', body)
    return data.data
  },
  socialLogin: async (provider: 'kakao' | 'naver' | 'google', body: SocialLoginRequest) => {
    const { data } = await apiClient.post<ApiResponse<SocialLoginResponse>>(
      `/auth/social-login/${provider}`,
      body,
    )
    return data.data
  },

  findId: async (body: FindIdRequest): Promise<FindIdResponse> => {
    const { data } = await apiClient.post<ApiResponse<FindIdResponse>>('/auth/find-id', body)
    return data.data
  },

  resetPassword: async (body: ResetPasswordRequest): Promise<void> => {
    await apiClient.post<ApiResponse<void>>('/auth/reset-password', body)
  },
}
