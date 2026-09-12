import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { useCartCount } from '@/features/cart/hooks'
import { SideMenu } from '@/shared/components/SideMenu'
import { getCountdownSeconds } from '@/shared/utils/saleSchedule'

interface HeaderProps {
  showCountdown?: boolean
  title?: string
  showBackButton?: boolean
}

export function Header({ showCountdown = true, title, showBackButton = false }: HeaderProps) {
  const [menuOpen, setMenuOpen] = useState(false)
  // SALE_START_AT(saleSchedule.ts) 기준 남은 초를 매 초 재계산 — 탭 복귀/일시 정지에도 정확.
  const [timeLeft, setTimeLeft] = useState(() => getCountdownSeconds())
  const cartCount = useCartCount()
  const navigate = useNavigate()

  useEffect(() => {
    if (!showCountdown) return
    const timer = setInterval(() => {
      setTimeLeft(getCountdownSeconds())
    }, 1000)
    return () => clearInterval(timer)
  }, [showCountdown])

  const formatTime = (seconds: number) => {
    // 1일 이상 남았으면 dd일 hh:mm:ss, 미만이면 hh:mm:ss
    const days = Math.floor(seconds / 86400)
    const remainder = seconds % 86400
    const h = Math.floor(remainder / 3600)
    const m = Math.floor((remainder % 3600) / 60)
    const s = remainder % 60
    const hms = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
    return days > 0 ? `${days}일 ${hms}` : hms
  }

  return (
    <>
      <header className="sticky top-0 z-40 w-full bg-footer-bg shadow-sm">
        {/* Countdown bar - Figma Node 100:42 */}
        {showCountdown && (
          <div className="flex h-[30px] items-center justify-center bg-main-bg">
            <span className="font-lexend text-[10px] font-semibold text-point uppercase tracking-tight">
              마이페이브 판매 시작까지{' '}
              <span className="font-bold">{formatTime(timeLeft)}</span>
            </span>
          </div>
        )}
        
        {/* Nav bar */}
        <div className="relative flex h-14 items-center px-[10px]">
          {/* Left Section: Back Button + Title OR Hamburger */}
          <div className="z-10 flex items-center gap-1">
            {showBackButton ? (
              <button
                onClick={() => navigate(-1)}
                className="flex h-[44px] items-center gap-1 px-2"
                aria-label="뒤로 가기"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" className="text-[#322927]">
                  <polyline points="15 18 9 12 15 6" />
                </svg>
                {title && (
                  <span className="font-noto text-[15px] font-medium leading-[30px] text-[#322927]">{title}</span>
                )}
              </button>
            ) : title ? (
               <div className="px-4">
                 <span className="font-noto text-[15px] font-medium leading-[30px] text-[#322927]">{title}</span>
               </div>
            ) : (
              <button
                className="flex h-11 w-11 items-center justify-center"
                onClick={() => setMenuOpen(true)}
                aria-label="메뉴 열기"
              >
                <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
                  <path d="M3.66577 10.9976H18.3292" stroke="currentColor" className="text-dark-text" strokeWidth="1.83293" strokeLinecap="round" strokeLinejoin="round" />
                  <path d="M3.66577 5.49878H18.3292" stroke="currentColor" className="text-dark-text" strokeWidth="1.83293" strokeLinecap="round" strokeLinejoin="round" />
                  <path d="M3.66577 16.4963H18.3292" stroke="currentColor" className="text-dark-text" strokeWidth="1.83293" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </button>
            )}
          </div>

          {/* Logo - Always Absolute Centered */}
          <div className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 pointer-events-none">
            <Link to="/" className="flex items-center pointer-events-auto">
              <img
                src="/logo.svg"
                alt="My Fave"
                className="h-[36px] w-auto transition-all"
              />
            </Link>
          </div>

          {/* Right Section: Cart icon (Only in Default Mode) */}
          {!title && (
            <div className="ml-auto z-10">
              <Link to="/cart" className="relative flex h-11 w-11 items-center justify-center">
                <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                  <path d="M4.99764 1.66589L2.49878 4.99771V16.6591C2.49878 17.1009 2.67429 17.5246 2.98671 17.837C3.29913 18.1495 3.72286 18.325 4.16469 18.325H15.826C16.2679 18.325 16.6916 18.1495 17.004 17.837C17.3164 17.5246 17.492 17.1009 17.492 16.6591V4.99771L14.9931 1.66589H4.99764Z" stroke="currentColor" className="text-dark-text" strokeWidth="1.66591" strokeLinecap="round" strokeLinejoin="round" />
                  <path d="M2.49878 4.99774H17.492" stroke="currentColor" className="text-dark-text" strokeWidth="1.66591" strokeLinecap="round" strokeLinejoin="round" />
                  <path d="M13.3272 8.32953C13.3272 9.21318 12.9762 10.0606 12.3513 10.6855C11.7265 11.3103 10.879 11.6613 9.99539 11.6613C9.11174 11.6613 8.26428 11.3103 7.63944 10.6855C7.0146 10.0606 6.66357 9.21318 6.66357 8.32953" stroke="currentColor" className="text-dark-text" strokeWidth="1.66591" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
                {cartCount > 0 && (
                  <span className="absolute top-[6px] right-[6px] flex h-[16px] w-[16px] items-center justify-center rounded-full bg-red-500 text-[10px] font-bold text-white shadow-sm ring-2 ring-footer-bg">
                    {cartCount > 99 ? '99+' : cartCount}
                  </span>
                )}
              </Link>
            </div>
          )}
        </div>
      </header>

      <SideMenu isOpen={menuOpen} onClose={() => setMenuOpen(false)} />
    </>
  )
}
