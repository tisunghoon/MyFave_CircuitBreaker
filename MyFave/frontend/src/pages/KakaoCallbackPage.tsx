import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'

import { useKakaoLogin } from '@/features/auth/hooks'
import { PopUp } from '@/shared/components/PopUp'

export function KakaoCallbackPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { mutate } = useKakaoLogin()
  const [isPopUpOpen, setIsPopUpOpen] = useState(false)
  const calledRef = useRef(false)

  useEffect(() => {
    if (calledRef.current) return
    const code = searchParams.get('code')
    if (!code) {
      navigate('/login', { replace: true })
      return
    }
    calledRef.current = true

    mutate(code, {
      onSuccess: () => navigate('/', { replace: true }),
      onError: () => setIsPopUpOpen(true),
    })
  }, [searchParams, mutate, navigate])

  return (
    <div className="flex min-h-screen items-center justify-center bg-white">
      <p className="font-noto text-[14px] text-[#322927]">
        카카오 로그인 처리 중...
      </p>
      <PopUp
        isOpen={isPopUpOpen}
        message="카카오 로그인에 실패했습니다. 다시 시도해주세요."
        onClose={() => {
          setIsPopUpOpen(false)
          navigate('/login', { replace: true })
        }}
      />
    </div>
  )
}
