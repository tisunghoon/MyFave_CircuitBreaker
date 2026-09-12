import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

import App from './app/App'
import './index.css'

declare global {
  interface Window {
    global?: typeof globalThis
    Kakao?: {
      init: (key: string) => void
      isInitialized: () => boolean
      Auth: {
        authorize: (params: { redirectUri: string; state?: string }) => void
      }
    }
  }
}

// Polyfill for SockJS in Vite environment
if (typeof window !== 'undefined' && window.global === undefined) {
  window.global = window
}

const kakaoKey = import.meta.env.VITE_KAKAO_JAVASCRIPT_KEY
if (kakaoKey && window.Kakao && !window.Kakao.isInitialized()) {
  window.Kakao.init(kakaoKey)
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
