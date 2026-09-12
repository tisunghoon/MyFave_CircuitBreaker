# MyFave 프론트엔드 초기세팅 Context

> **대상**: MyFave 프로젝트의 프론트엔드 초기세팅을 진행하는 백엔드 개발자 및 프론트엔드 AI 에이전트
**기술 스택**: React 18+, TypeScript 5+, Vite, yarn (berry/classic 무관)
**작성일 기준 가정**: 웹 기반 SPA. React Native(Expo)로 가는 경우 §10 참고.
> 

---

## 0. 사용 기술스택

| 구분 | React(웹) |
| --- | --- |
| 런타임 | 브라우저 |
| 번들러 | Vite |
| 라우팅 | react-router-dom |
| 스타일 | CSS / Tailwind |
| 배포 | Vercel/Netlify/S3+CF |
| Figma 프로토타입 | 모바일 사이즈지만 웹 모바일뷰로 구현 가능 |

인플루언서 플리마켓 특성상 "인스타 링크 → 즉시 진입 → 결제" 플로우가 핵심인데, **앱 설치 장벽이 있는 RN보다 웹이 전환율에 유리**할 수 있습니다. 반대로 푸시 알림/카메라/딥링크 등 네이티브 기능이 필요하면 RN이 낫고요. 한 번 더 판단해보세요.

---

## 1. 필수 사전 설치

아래 도구들을 순서대로 설치합니다. macOS 기준 명령어를 병기합니다.

### 1-1. Node.js (LTS)

프론트엔드는 Node.js 위에서 돌아갑니다. 버전 관리자인 `nvm` 사용을 강력 권장합니다 (프로젝트마다 Node 버전이 다를 수 있음).

```bash
# nvm 설치
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash

# 터미널 재시작 후
nvm install --lts       # 최신 LTS 설치
nvm use --lts
node -v                 # v20.x.x 같은 버전이 나와야 정상
```

### 1-2. yarn

필수 스택에 명시되어 있으므로 yarn을 씁니다. Corepack을 통해 설치하는 방식이 가장 깔끔합니다 (Node 16.10+ 내장).

```bash
corepack enable
corepack prepare yarn@stable --activate
yarn -v                 # 4.x.x 가 나오면 yarn berry, 1.x.x 면 classic
```

> **berry vs classic**: berry(2+)는 기능이 많고 빠르지만 설정이 까다롭습니다. 팀 전체가 berry에 익숙하지 않다면 `yarn@1.22.22` (classic)으로 통일하는 것도 방법입니다. 중요한 건 **팀 전원이 같은 버전을 쓰는 것**입니다. `package.json`에 `"packageManager": "yarn@1.22.22"`처럼 박아두면 Corepack이 자동으로 맞춰줍니다.
> 

### 1-3. VSCode + 필수 확장

- **ESLint**: 코드 스타일 검사
- **Prettier**: 코드 포맷팅
- **Error Lens**: 에러/경고를 라인 옆에 바로 표시
- **Pretty TypeScript Errors**: TS 에러를 읽기 쉽게
- **GitLens**: 이전 파일 수정 내역 확인

---

## 2. 프로젝트 생성 (Vite)

Vite는 현재 React 프로젝트의 사실상 표준 번들러입니다. 빠르고 설정이 간단합니다.

```bash
# MyFave 프로젝트 폴더로 이동 후
yarn create vite myfave-frontend --template react-ts
cd myfave-frontend
yarn install
yarn dev
```

`http://localhost:5173`이 열리면 성공. 기본 Vite 화면이 보입니다.

### 2-1. 폴더 구조 세팅

Vite 기본 구조를 다음처럼 바꿉니다. 백엔드의 도메인별 패키지 구조(controller/service/repository)와 비슷한 철학으로, **기능(feature) 단위로 분리**합니다.

```
myfave-frontend/
├── public/                      # 정적 파일 (favicon 등)
├── src/
│   ├── app/                     # 앱 진입점, 전역 프로바이더
│   │   ├── App.tsx
│   │   ├── router.tsx           # 라우팅 설정
│   │   └── providers.tsx        # QueryClient, Theme 등 Provider 모음
│   ├── pages/                   # 라우트 단위 페이지 (화면 한 장 = 파일 한 개)
│   │   ├── LoginPage.tsx
│   │   ├── MainPage.tsx
│   │   ├── ProductListPage.tsx
│   │   ├── ProductDetailPage.tsx
│   │   ├── CartPage.tsx
│   │   ├── PaymentPage.tsx
│   │   └── MyPage.tsx
│   ├── features/                # 도메인별 로직 (API 명세서의 도메인과 1:1 매핑)
│   │   ├── auth/
│   │   │   ├── api.ts           # axios 호출 함수
│   │   │   ├── hooks.ts         # useLogin, useSignup 등 React Query 훅
│   │   │   ├── types.ts         # 요청/응답 타입
│   │   │   └── components/      # 이 도메인에만 쓰이는 컴포넌트
│   │   ├── products/
│   │   ├── cart/
│   │   ├── orders/
│   │   ├── payments/
│   │   ├── shipping/
│   │   ├── coupons/
│   │   ├── contents/
│   │   ├── sale-events/
│   │   └── chat/                # WebSocket 포함
│   ├── shared/                  # 전역 재사용 요소
│   │   ├── api/
│   │   │   ├── axios.ts         # axios 인스턴스 + 인터셉터
│   │   │   └── types.ts         # 공통 응답 타입 (ApiResponse<T>, Pageable 등)
│   │   ├── components/          # Button, Input, Modal, CountdownBar 등
│   │   ├── hooks/               # useDebounce, useIntersectionObserver 등
│   │   ├── lib/                 # 유틸 함수 (formatPrice, formatDate 등)
│   │   ├── constants/           # ENUM 값, 라우트 경로 상수
│   │   └── styles/              # 전역 스타일, 디자인 토큰
│   ├── assets/                  # 이미지, 폰트, 아이콘
│   ├── main.tsx                 # React 루트 렌더링
│   └── vite-env.d.ts
├── .env.development             # 개발용 환경변수
├── .env.production              # 운영용 환경변수
├── .eslintrc.cjs
├── .prettierrc
├── tsconfig.json
├── vite.config.ts
└── package.json
```

### 2-2. 경로 Alias 설정

`@/features/auth/api` 처럼 `@`로 src를 가리킬 수 있게 합니다. 상대경로 지옥(`../../../`)을 피할 수 있습니다.

**`tsconfig.json`**

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "lib": ["ES2022", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "moduleResolution": "bundler",
    "jsx": "react-jsx",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  },
  "include": ["src"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

**`vite.config.ts`**

```tsx
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      // 개발 중 CORS 회피: /api 요청을 백엔드로 프록시
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

---

## 3. 필수 라이브러리 설치

역할별로 묶어서 설명합니다. **모두 필요하진 않습니다** — 최소셋(★)만 먼저 깔고, 나머지는 필요할 때 추가하세요.

### 3-1. 라우팅 ★

```bash
yarn add react-router-dom
```

- 페이지 이동, URL 파라미터, 보호된 라우트(로그인 필요) 구현

### 3-2. 서버 상태 관리 ★

```bash
yarn add @tanstack/react-query
yarn add -D @tanstack/react-query-devtools
```

- **왜 필수?** 백엔드 API 호출 결과를 캐싱하고, 로딩/에러 상태를 자동 관리해줍니다. 직접 useState/useEffect로 구현하면 코드가 3배로 늘어납니다.
- MyFave의 상품 목록, 주문 내역, 쿠폰 조회 등 거의 모든 GET 요청에 사용

### 3-3. HTTP 클라이언트 ★

```bash
yarn add axios
```

- fetch보다 인터셉터(토큰 자동 주입, 에러 공통 처리)가 편합니다.

### 3-4. 클라이언트 상태 관리 ★

```bash
yarn add zustand
```

- Redux보다 훨씬 간단. 로그인 유저 정보, 장바구니 임시 상태 등을 전역으로 관리할 때 사용.
- React Query(서버 상태) + Zustand(클라이언트 상태) 조합이 현재 표준입니다.

### 3-5. 폼 & 검증 ★

```bash
yarn add react-hook-form zod @hookform/resolvers
```

- 로그인, 회원가입, 배송지 입력 등 폼 처리.
- `zod`로 스키마 작성하면 TypeScript 타입도 자동 추론됩니다.

### 3-6. 스타일링 (택 1) ★

**옵션 A: Tailwind CSS (권장)**

```bash
yarn add -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
```

- 빠르고, Figma 디자인 토큰을 config에 넣기 좋음
- MyFave 핑크 컬러 팔레트를 `tailwind.config.js`의 `theme.extend.colors`에 등록

**옵션 B: styled-components**

```bash
yarn add styled-components
yarn add -D @types/styled-components
```

- CSS-in-JS. 동적 스타일이 많을 때 유리

**옵션 C: CSS Modules** (Vite 기본 지원, 추가 설치 불필요)

- 가장 단순. 러닝 커브 없음

### 3-7. WebSocket (채팅)

```bash
yarn add @stomp/stompjs sockjs-client
yarn add -D @types/sockjs-client
```

- API 명세서 §12에서 `wss://domain/ws/chat` 쓰므로 Spring WebSocket의 STOMP 프로토콜과 궁합 맞는 라이브러리.
- **주의**: 백엔드에서 STOMP를 쓰지 않고 raw WebSocket으로 가면 `socket.io-client`나 브라우저 내장 `WebSocket` API를 대신 씁니다. 백엔드 팀과 합의 필요.

### 3-8. 날짜 처리

```bash
yarn add date-fns
```

- 카운트다운 타이머 바, ISO 8601 파싱 등에 사용. moment.js보다 가볍고 트리쉐이킹 가능.

### 3-9. 기타 유용한 것

```bash
yarn add react-intersection-observer   # 무한스크롤용
yarn add clsx                          # 조건부 className 합치기
```

---

## 4. 코드 품질 도구 (ESLint + Prettier)

팀 작업 시 필수. 코드 스타일을 통일해서 PR 리뷰 시간을 줄입니다.

```bash
yarn add -D eslint prettier eslint-config-prettier eslint-plugin-prettier
yarn add -D eslint-plugin-react eslint-plugin-react-hooks
yarn add -D @typescript-eslint/eslint-plugin @typescript-eslint/parser
yarn add -D eslint-plugin-import eslint-import-resolver-typescript
```

**`.eslintrc.cjs`**

```jsx
module.exports = {
  root: true,
  env: { browser: true, es2022: true },
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:react/recommended',
    'plugin:react-hooks/recommended',
    'plugin:import/recommended',
    'plugin:import/typescript',
    'prettier',
  ],
  parser: '@typescript-eslint/parser',
  parserOptions: { ecmaVersion: 'latest', sourceType: 'module' },
  plugins: ['react', '@typescript-eslint', 'import'],
  settings: {
    react: { version: 'detect' },
    'import/resolver': { typescript: {} },
  },
  rules: {
    'react/react-in-jsx-scope': 'off',   // Vite는 자동 import
    'import/order': ['warn', {
      groups: ['builtin', 'external', 'internal', 'parent', 'sibling'],
      'newlines-between': 'always',
      alphabetize: { order: 'asc' },
    }],
  },
}
```

**`.prettierrc`**

```json
{
  "semi": false,
  "singleQuote": true,
  "trailingComma": "all",
  "printWidth": 100,
  "tabWidth": 2,
  "arrowParens": "always"
}
```

**`package.json` 스크립트 추가**

```json
{
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "lint": "eslint . --ext ts,tsx --report-unused-disable-directives --max-warnings 0",
    "lint:fix": "eslint . --ext ts,tsx --fix",
    "format": "prettier --write \"src/**/*.{ts,tsx,css,md}\""
  }
}
```

---

## 5. 환경변수 설정

백엔드 API Base URL을 하드코딩하지 말고 환경변수로 관리합니다.

**`.env.development`**

```
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_WS_BASE_URL=ws://localhost:8080/ws
```

**`.env.production`**

```
VITE_API_BASE_URL=https://api.myfave.com/api/v1
VITE_WS_BASE_URL=wss://api.myfave.com/ws
```

> Vite에서는 반드시 `VITE_` 접두사가 붙어야 클라이언트에서 읽을 수 있습니다. `import.meta.env.VITE_API_BASE_URL`로 접근.
> 

**`.gitignore`에 추가**

```
.env.local
.env.*.local
```

---

## 6. 핵심 보일러플레이트 코드

### 6-1. axios 인스턴스 (`src/shared/api/axios.ts`)

```tsx
import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios'

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
})

// 요청 인터셉터: JWT 자동 주입
apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('accessToken')
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 응답 인터셉터: 401 발생 시 토큰 재발급
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      const refreshToken = localStorage.getItem('refreshToken')

      if (refreshToken) {
        try {
          const { data } = await axios.post(
            `${import.meta.env.VITE_API_BASE_URL}/auth/reissue`,
            { refreshToken },
          )
          localStorage.setItem('accessToken', data.data.accessToken)
          localStorage.setItem('refreshToken', data.data.refreshToken)
          originalRequest.headers.Authorization = `Bearer ${data.data.accessToken}`
          return apiClient(originalRequest)
        } catch (e) {
          localStorage.clear()
          window.location.href = '/login'
        }
      }
    }
    return Promise.reject(error)
  },
)
```

### 6-2. 공통 응답 타입 (`src/shared/api/types.ts`)

API 명세서의 공통 응답 구조와 1:1 매핑합니다.

```tsx
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
}

export interface ApiError {
  code: number
  message: string
  errorCode: string
}
```

### 6-3. React Query Provider (`src/app/providers.tsx`)

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { ReactNode } from 'react'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60,     // 1분간 fresh
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

export function Providers({ children }: { children: ReactNode }) {
  return (
    <QueryClientProvider client={queryClient}>
      {children}
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  )
}
```

### 6-4. 라우터 (`src/app/router.tsx`)

```tsx
import { createBrowserRouter, RouterProvider, Navigate } from 'react-router-dom'
import { LoginPage } from '@/pages/LoginPage'
import { MainPage } from '@/pages/MainPage'
// ...

const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  { path: '/', element: <MainPage /> },
  { path: '/products', element: <ProductListPage /> },
  { path: '/products/:productId', element: <ProductDetailPage /> },
  { path: '/cart', element: <CartPage /> },
  { path: '/payment', element: <PaymentPage /> },
  { path: '/mypage', element: <MyPage /> },
  { path: '*', element: <Navigate to="/" /> },
])

export function AppRouter() {
  return <RouterProvider router={router} />
}
```

### 6-5. 예시: Auth 도메인 (`src/features/auth/`)

**`types.ts`**

```tsx
export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  userId: number
  nickname: string
}
```

**`api.ts`**

```tsx
import { apiClient } from '@/shared/api/axios'
import type { ApiResponse } from '@/shared/api/types'
import type { LoginRequest, LoginResponse } from './types'

export const authApi = {
  login: async (body: LoginRequest) => {
    const { data } = await apiClient.post<ApiResponse<LoginResponse>>('/auth/login', body)
    return data.data
  },
}
```

**`hooks.ts`**

```tsx
import { useMutation } from '@tanstack/react-query'
import { authApi } from './api'

export function useLogin() {
  return useMutation({
    mutationFn: authApi.login,
    onSuccess: (data) => {
      localStorage.setItem('accessToken', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
    },
  })
}
```

---

## 7. 백엔드 API 명세서 → 프론트 타입 자동화 (선택 사항, 강력 추천)

MyFave API 명세서가 이미 잘 정리되어 있으므로, Swagger/OpenAPI 스펙만 백엔드가 만들어주면 프론트 타입을 자동 생성할 수 있습니다. 수작업 타입 정의의 70%를 절약합니다.

```bash
yarn add -D openapi-typescript
```

```bash
# 백엔드가 http://localhost:8080/v3/api-docs 로 스펙을 내려준다고 가정
yarn openapi-typescript http://localhost:8080/v3/api-docs -o src/shared/api/schema.ts
```

그러면 `schema.ts`에서 모든 API의 요청/응답 타입이 자동 생성됩니다. 백엔드 수정 시 이 명령만 다시 돌리면 끝.

---

## 8. 실행 체크리스트

초기세팅이 끝났는지 확인하는 순서:

```bash
yarn dev              # 1. localhost:5173 에 접속되는가
yarn lint             # 2. 린트 에러 0개인가
yarn build            # 3. 프로덕션 빌드 성공하는가
```

모두 통과하면 첫 커밋.

```bash
git init
git add .
git commit -m "chore: initial frontend setup with vite + react + ts"
```

---

## 9. 개발 순서 추천

백엔드 도메인 개발과 병렬로 움직이는 걸 권장합니다. 백엔드 API가 없어도 React Query의 `queryFn`에 임시 mock 데이터를 반환하게 하면 UI를 먼저 만들 수 있습니다.

1. **환경세팅 + 공통 레이아웃** (헤더, 카운트다운 타이머 바, 푸터) — 0.5일
2. **Login / 회원가입** — 백엔드 Auth API와 동시 — 1일
3. **Main 페이지** (스타일 피드, Influencer's PICK) — 1일
4. **Product List / Detail** — 1.5일
5. **Cart / Payment** — 2일 (결제 연동 포함)
6. **My page / 배송지 / 쿠폰** — 1일
7. **채팅 (WebSocket)** — 2일 — 가장 나중에

---

## 10. 막힐 때 참고할 것

- **React 공식 문서 (한국어)**: https://ko.react.dev/
- **TanStack Query 공식**: https://tanstack.com/query/latest/docs/framework/react/overview
- **Vite 공식**: https://vitejs.dev/
- **React Hook Form**: https://react-hook-form.com/
- **Zustand**: https://zustand.docs.pmnd.rs/