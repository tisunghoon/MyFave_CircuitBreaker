import { useEffect, useLayoutEffect } from 'react'
import { Outlet, useLocation } from 'react-router-dom'

// 라우트 변경 시 페이지 최상단으로 자동 스크롤.
// 단일 useLayoutEffect 만으로는 다음 케이스에서 스크롤이 0 으로 안 가는 현상 발생:
//   - 새 페이지가 useQuery 등 비동기 데이터를 기다리는 동안 placeholder → 실제 컨텐츠 교체로 layout shift
//   - SmartImage(heic2any) async 이미지 변환 직후 layout jump
//   - 브라우저 자체의 scroll restoration 이 뒤늦게 개입
// → useLayoutEffect(즉시) + useEffect(paint 후) + rAF(다음 frame) 다중 패스로 방어.
export function ScrollToTopWrapper() {
  const { pathname } = useLocation()

  // 1) 페인트 직전 — 새 페이지가 그려지기 전에 스크롤 위치 0 으로 설정.
  useLayoutEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: 'instant' })
  }, [pathname])

  // 2) 페인트 직후 + 다음 frame — 비동기 컨텐츠 마운트나 layout shift 가 끝난 시점에 한 번 더 강제 reset.
  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: 'instant' })
    const raf = requestAnimationFrame(() => {
      window.scrollTo({ top: 0, left: 0, behavior: 'instant' })
    })
    return () => cancelAnimationFrame(raf)
  }, [pathname])

  return <Outlet />
}
