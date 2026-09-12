import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { useResetPassword } from '@/features/auth/hooks'
import { PopUp } from '@/shared/components/PopUp'
import { isValidEmail } from '@/shared/utils/validation'

export function FindPasswordPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [popUpMessage, setPopUpMessage] = useState('')
  const [isPopUpOpen, setIsPopUpOpen] = useState(false)
  const resetPasswordMutation = useResetPassword()

  const showPopUp = (msg: string) => {
    setPopUpMessage(msg)
    setIsPopUpOpen(true)
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!email) { showPopUp('이메일을 입력해주세요'); return }
    if (!isValidEmail(email)) { showPopUp('올바른 이메일 형식이 아닙니다'); return }

    resetPasswordMutation.mutate(
      { email },
      {
        onSuccess: () => {
          showPopUp('임시 비밀번호가 이메일로 발송되었습니다 🔑')
          setTimeout(() => navigate('/login'), 2200)
        },
        onError: (error) => {
          const errorCode = (error as { response?: { data?: { errorCode?: string } } })
            ?.response?.data?.errorCode
          if (errorCode === 'USER_NOT_FOUND') {
            showPopUp('가입되지 않은 이메일입니다')
          } else if (errorCode === 'EMAIL_SEND_FAILED') {
            showPopUp('메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요')
          } else {
            showPopUp('임시 비밀번호 발송에 실패했습니다')
          }
        },
      },
    )
  }

  return (
    <div className="flex min-h-screen flex-col items-center bg-white pt-[31px]">
      <div className="w-[320px] mb-3">
        <button
          type="button"
          onClick={() => navigate('/login')}
          className="flex items-center gap-1 font-noto text-[13px] font-medium text-[#8B7E74] active:opacity-60 transition-opacity"
          aria-label="로그인 화면으로 돌아가기"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="15 18 9 12 15 6" />
          </svg>
          <span>뒤로</span>
        </button>
      </div>

      <div className="w-[320px]">
        <div className="mb-[64px] flex justify-center">
          <img src="/logo.svg" alt="My Fave" className="h-[77px] w-[118px] object-contain" />
        </div>

        <h1 className="mb-[11px] font-noto text-[24px] font-bold text-black">비밀번호 찾기</h1>
        <p className="mb-[18px] font-noto text-[13px] text-[#8B7E74] leading-[20px]">
          가입한 이메일로 임시 비밀번호를 발송해드립니다.
          <br />
          로그인 후 즉시 비밀번호를 변경해주세요.
        </p>

        <form onSubmit={handleSubmit} className="space-y-[11px]">
          <div className="flex h-[43px] items-center rounded-[5px] border border-[#BBBBBB] bg-white px-[19px]">
            <input
              type="email"
              placeholder="이메일을 입력해주세요"
              className="w-full bg-transparent font-noto text-[16px] font-medium text-[#322927] placeholder:text-[#999999] focus:outline-none"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>

          <button
            type="submit"
            disabled={!email || resetPasswordMutation.isPending}
            className="flex h-[43px] w-full items-center justify-center rounded-[5px] bg-point font-noto text-[16px] font-medium text-white shadow-sm active:scale-[0.98] transition-transform disabled:bg-gray-300"
          >
            {resetPasswordMutation.isPending ? '발송 중...' : '임시 비밀번호 받기'}
          </button>
        </form>

        <div className="mt-[20px] text-center">
          <Link to="/find-id" className="font-noto text-[12px] font-bold text-chat-font hover:underline decoration-chat-font/30">
            아이디 찾기
          </Link>
        </div>
      </div>

      <PopUp isOpen={isPopUpOpen} message={popUpMessage} onClose={() => setIsPopUpOpen(false)} />
    </div>
  )
}
