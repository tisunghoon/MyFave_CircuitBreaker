export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  userId: number
  nickname: string
  profileImageUrl?: string
}

export interface User {
  id: number
  email: string
  nickname: string
  // 회원가입 시 무작위로 부여되는 곰돌이 프로필 이미지 URL (S3).
  // 풀이 비어있거나 백엔드가 아직 필드를 내려주지 않으면 undefined.
  profileImageUrl?: string
}

export interface SocialLoginRequest {
  authorizationCode: string
}

export interface SocialLoginResponse {
  accessToken: string
  refreshToken: string
  userId: number
  nickname: string
  email: string
  isNewUser: boolean
  profileImageUrl?: string
}

export interface SignUpSendCodeRequest {
  email: string
}

export interface SignUpVerifyCodeRequest {
  email: string
  verificationCode: string
}

export interface SignUpVerifyCodeResponse {
  verifiedToken: string
}

export interface SignUpRequest {
  email: string
  password: string
  name: string
  nickname: string
  phone: string
  verifiedToken: string
  // ─ profileImageUrl 은 백엔드 미지원이므로 전송하지 않음 ─
  // 회원가입 시 부여된 곰돌이 아바타는 localStorage(PENDING_AVATAR_KEY) 에 저장해 다음 로그인 시 복원.
  // 백엔드가 SignUpRequest DTO 에 필드를 추가하기 전까지 프론트는 이 필드를 보내지 않는다 (CR PR#185 M4).
}

export interface SignUpResponse {
  userId: number
  nickname: string
  // 응답 측은 백엔드가 추가하기 시작하면 그대로 활용 — 응답 unknown 필드는 클라이언트에서 무시되므로 안전.
  profileImageUrl?: string
}

export interface FindIdRequest {
  name: string
  phoneNumber: string
}

export interface FindIdResponse {
  maskedEmail: string
}

export interface ResetPasswordRequest {
  email: string
}
