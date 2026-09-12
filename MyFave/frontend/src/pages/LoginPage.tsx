import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { useLogin } from '@/features/auth/hooks'
import { PopUp } from '@/shared/components/PopUp'
import { isValidEmail } from '@/shared/utils/validation'

export function LoginPage() {
  const navigate = useNavigate()
  const { mutate: loginMutate, isPending } = useLogin()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [autoLogin, setAutoLogin] = useState(false)
  const [isPopUpOpen, setIsPopUpOpen] = useState(false)
  const [popUpMessage, setPopUpMessage] = useState('')

  const showPopUp = (msg: string) => {
    setPopUpMessage(msg)
    setIsPopUpOpen(true)
  }

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault()

    if (!email || !password) {
      showPopUp('이메일과 비밀번호를 모두 입력해주세요')
      return
    }
    if (!isValidEmail(email)) {
      showPopUp('올바른 이메일 형식이 아닙니다')
      return
    }

    loginMutate(
      { email, password },
      {
        onSuccess: () => navigate('/'),
        onError: () => showPopUp('이메일 또는 비밀번호가 일치하지 않습니다'),
      },
    )
  }

  const handleKakaoLogin = () => {
    if (!window.Kakao?.isInitialized?.()) {
      showPopUp('카카오 로그인을 사용할 수 없습니다.')
      return
    }
    window.Kakao.Auth.authorize({
      redirectUri: `${window.location.origin}/auth/kakao/callback`,
    })
  }

  return (
    <div className="flex min-h-screen flex-col items-center bg-white">
      {/* Container - Figma Node 98:92 */}
      <div className="flex w-[320px] flex-col items-center pt-[96px] pb-10">
        {/* Logo - Figma Node 284:69 */}
        <div className="mb-[136px] h-[125px] w-[192px]">
          <img
            src="/logo.svg"
            alt="My Fave"
            className="h-full w-full object-contain"
          />
        </div>

        {/* Form Container - Figma Node 98:91 */}
        <form onSubmit={handleLogin} className="w-full space-y-[15px]">
          {/* Input Stack - Figma Node 98:89 */}
          <div className="flex flex-col">
            {/* Email - Figma Node 98:87 */}
            <div className="flex h-[43px] items-center rounded-t-[8px] border border-[#BBBBBB] bg-white px-[14px]">
              <input
                type="email"
                placeholder="이메일"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full bg-transparent font-noto text-[14px] font-medium text-[#322927] placeholder:text-[#999999] focus:outline-none"
              />
            </div>
            {/* Password - Figma Node 98:88 */}
            <div className="mt-[-1px] flex h-[43px] items-center rounded-b-[8px] border border-[#BBBBBB] bg-white px-[14px]">
              <input
                type="password"
                placeholder="패스워드"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full bg-transparent font-noto text-[14px] font-medium text-[#322927] placeholder:text-[#999999] focus:outline-none"
              />
            </div>
          </div>

          {/* Login Button - Figma Node 98:90 */}
          <button
            type="submit"
            disabled={isPending}
            className="flex h-[43px] w-full items-center justify-center rounded-[5px] bg-point font-noto text-[16px] font-medium text-white shadow-sm active:scale-[0.98] transition-transform disabled:opacity-60"
          >
            {isPending ? '로그인 중...' : '로그인'}
          </button>

          {/* Kakao Login Button */}
          <button
            type="button"
            onClick={handleKakaoLogin}
            className="flex h-[43px] w-full items-center justify-center gap-[8px] rounded-[5px] bg-[#FEE500] font-noto text-[16px] font-medium text-[rgba(0,0,0,0.85)] active:scale-[0.98] transition-transform"
          >
            <svg width="18" height="17" viewBox="0 0 18 17" fill="none" aria-hidden="true">
              <path
                d="M9 0C4.03 0 0 3.13 0 7c0 2.5 1.65 4.7 4.13 5.95-.18.65-.66 2.41-.75 2.78-.12.46.17.46.36.34.15-.1 2.4-1.62 3.36-2.27.61.09 1.24.13 1.9.13 4.97 0 9-3.13 9-7s-4.03-7-9-7z"
                fill="currentColor"
              />
            </svg>
            카카오로 시작하기
          </button>

          {/* Demo Account Info - added for demonstration */}
          <p className="text-center font-noto text-[12px] text-muted-text/80">
            시연 계정: test@test.com / password
          </p>

          {/* Options & Links */}
          <div className="space-y-[15px] pt-[2px]">
            {/* Auto Login - Figma Node 148:180 */}
            <div className="flex items-center gap-[8px]">
              <div className="relative flex items-center h-[17px]">
                <input
                  type="checkbox"
                  id="autoLogin"
                  checked={autoLogin}
                  onChange={(e) => setAutoLogin(e.target.checked)}
                  className="h-[17px] w-[17px] cursor-pointer rounded-[4px] border border-[#CFCFCF] bg-white text-point focus:ring-0"
                />
              </div>
              <label
                htmlFor="autoLogin"
                className="cursor-pointer font-noto text-[14px] font-medium leading-[18px] text-[#999999]"
              >
                자동로그인
              </label>
            </div>

            {/* Bottom Links Row - Figma Node 98:86 */}
            <div className="flex items-center justify-between">
              {/* Find ID/PW - Figma Node 98:85 */}
              <div className="flex items-center gap-[15px]">
                <Link
                  to="/find-id"
                  className="font-noto text-[14px] font-medium text-[#999999] hover:text-[#322927] transition-colors"
                >
                  아이디찾기
                </Link>
                <div className="h-[10px] w-[1px] bg-[#EEEEEE]" />
                <Link
                  to="/find-password"
                  className="font-noto text-[14px] font-medium text-[#999999] hover:text-[#322927] transition-colors"
                >
                  비밀번호찾기
                </Link>
              </div>

              {/* Sign Up Link - Figma Node 98:84 */}
              <Link
                to="/signup"
                className="flex items-center gap-[4px] group"
              >
                <span className="font-noto text-[14px] font-bold text-chat-font group-hover:underline decoration-chat-font/30">회원가입</span>
                <svg width="5" height="8" viewBox="0 0 5 8" fill="none" className="transition-transform group-hover:translate-x-0.5">
                  <path d="M1 1L4 4L1 7" stroke="#CF879B" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
              </Link>
            </div>
          </div>
        </form>
      </div>

      <PopUp isOpen={isPopUpOpen} message={popUpMessage} onClose={() => setIsPopUpOpen(false)} />
    </div>
  )
}
