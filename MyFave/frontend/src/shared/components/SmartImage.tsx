import { useEffect, useRef, useState } from 'react'
import type { ImgHTMLAttributes } from 'react'

// .heic URL → 변환된 jpeg blob URL 캐시 (세션 동안 재사용)
const heicCache = new Map<string, string>()
const inFlight = new Map<string, Promise<string>>()

// HEIC 변환은 메인 스레드를 무겁게 점유하므로 동시 실행 수를 제한해 UI 응답성을 보호한다.
const MAX_CONCURRENT_CONVERSIONS = 2
let activeConversions = 0
const conversionWaitQueue: Array<() => void> = []

async function acquireConversionSlot(): Promise<() => void> {
  if (activeConversions >= MAX_CONCURRENT_CONVERSIONS) {
    await new Promise<void>((resolve) => conversionWaitQueue.push(resolve))
  }
  activeConversions += 1
  return () => {
    activeConversions -= 1
    const next = conversionWaitQueue.shift()
    if (next) next()
  }
}

// heic2any 청크(약 1.35MB)는 무겁다. 첫 SmartImage 가 마운트되는 즉시 prefetch 해 두면
// 실제 변환이 필요한 시점의 module-load 지연이 사라진다.
let heic2anyPrefetched = false
function prefetchHeic2any() {
  if (heic2anyPrefetched) return
  heic2anyPrefetched = true
  void import('heic2any').catch(() => {
    heic2anyPrefetched = false
  })
}

function isHeic(url: string): boolean {
  return /\.heic(\?|$)/i.test(url)
}

async function convertHeicUrl(url: string): Promise<string> {
  const cached = heicCache.get(url)
  if (cached) return cached
  const pending = inFlight.get(url)
  if (pending) return pending

  // 변환 전체 (fetch + heic2any) 를 semaphore 안에서 진행 — 메인 스레드 보호.
  // 성공/실패와 무관하게 finally 에서 inFlight 정리 (실패 후 재시도 영구 차단 방지).
  const promise = (async () => {
    const releaseSlot = await acquireConversionSlot()
    try {
      const res = await fetch(url)
      if (!res.ok) throw new Error(`fetch failed: ${res.status}`)
      const blob = await res.blob()
      const { default: heic2any } = await import('heic2any')
      const out = await heic2any({ blob, toType: 'image/jpeg', quality: 0.9 })
      const finalBlob: Blob | undefined = Array.isArray(out) ? out[0] : out
      if (!finalBlob) throw new Error('heic2any returned empty result')
      const objectUrl = URL.createObjectURL(finalBlob)
      heicCache.set(url, objectUrl)
      return objectUrl
    } finally {
      releaseSlot()
    }
  })().finally(() => {
    inFlight.delete(url)
  })

  inFlight.set(url, promise)
  return promise
}

interface SmartImageProps extends Omit<ImgHTMLAttributes<HTMLImageElement>, 'src'> {
  src: string | undefined
}

export function SmartImage({ src, ...rest }: SmartImageProps) {
  const needsConversion = src != null && isHeic(src)
  const [convertedUrl, setConvertedUrl] = useState<string | undefined>(undefined)
  const imgRef = useRef<HTMLImageElement>(null)
  // viewport 안에 들어왔는지 — HEIC 변환 트리거 게이트로 사용.
  // 캐시 hit 이면 즉시 true (관측 없이 바로 표시).
  const [isInView, setIsInView] = useState<boolean>(() =>
    needsConversion && src != null && heicCache.has(src),
  )

  // 첫 마운트 시 heic2any 청크 prefetch (HEIC src 가 있을 때만).
  useEffect(() => {
    if (needsConversion) prefetchHeic2any()
  }, [needsConversion])

  // IntersectionObserver — 화면 진입 200px 전에 변환 트리거. HEIC 만 적용.
  useEffect(() => {
    if (!needsConversion || isInView) return
    const el = imgRef.current
    if (!el) return
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((e) => e.isIntersecting)) {
          setIsInView(true)
          observer.disconnect()
        }
      },
      { rootMargin: '200px' },
    )
    observer.observe(el)
    return () => observer.disconnect()
  }, [needsConversion, isInView])

  useEffect(() => {
    // src 변경 시 이전 변환 URL 이 잠시 노출되는 것을 막기 위해 변환 시작 전에 초기화 (CR M8).
    // (의도된 동기 setState — src 가 바뀐 직후 한 번만 reset 하기 위함이며 의존성 배열로 가드됨)
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setConvertedUrl(undefined)
    if (!needsConversion || !src || !isInView) return
    let cancelled = false
    convertHeicUrl(src)
      .then((url) => {
        if (!cancelled) setConvertedUrl(url)
      })
      .catch(() => {
        if (!cancelled) setConvertedUrl(undefined)
      })
    return () => {
      cancelled = true
    }
  }, [src, needsConversion, isInView])

  // HEIC가 아니면 src 그대로 사용. HEIC면 변환 완료 전엔 undefined.
  const resolved = needsConversion ? convertedUrl : src

  // loading="lazy" — 브라우저 native lazy load 로 viewport 밖 이미지 fetch 자체를 지연.
  // HEIC 가 아닌 일반 jpg/png 에도 동일하게 적용되어 초기 로딩 부담 감소.
  return <img ref={imgRef} src={resolved} loading="lazy" decoding="async" {...rest} />
}
