// 인증/사용자 식별과 관련된 localStorage 키 상수.
// store.ts ↔ hooks.ts 양쪽에서 공유되므로 circular import 회피를 위해 별도 모듈로 분리.

// 회원가입 시 부여된 곰돌이 아바타 URL 을 다음 로그인까지 임시 보관하는 단일 비식별 키.
// 값 형식: JSON.stringify({ pendingEmail: string, url: string })
export const PENDING_AVATAR_KEY = 'myfave-pending-avatar'

// 레거시 키 prefix — 이메일을 키에 포함하던 이전 방식(`myfave-avatar:{email}`).
// 단일 키(PENDING_AVATAR_KEY) 로 마이그레이션됐고, clearUserStores 에서만 잔존 정리 목적으로 참조.
export const LEGACY_AVATAR_PREFIX = 'myfave-avatar:'
