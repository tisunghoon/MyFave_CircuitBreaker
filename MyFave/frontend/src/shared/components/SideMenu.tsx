import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { useIsAuthenticated } from '@/features/auth/hooks'
import { Modal } from '@/shared/components/Modal'

interface SideMenuProps {
  isOpen: boolean
  onClose: () => void
}

export function SideMenu({ isOpen, onClose }: SideMenuProps) {
  const navigate = useNavigate()
  const isAuthenticated = useIsAuthenticated()
  const [shopOpen, setShopOpen] = useState(true)
  const [communityOpen, setCommunityOpen] = useState(true)
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false)

  const handleProtectedNavigation = (path: string) => {
    if (!isAuthenticated) {
      setIsAuthModalOpen(true)
      return
    }
    onClose()
    navigate(path)
  }

  return (
    <>
      <Modal
        isOpen={isAuthModalOpen}
        onClose={() => setIsAuthModalOpen(false)}
        buttonText="확인"
        onButtonClick={() => {
          setIsAuthModalOpen(false)
          onClose()
          navigate('/login')
        }}
      >
        로그인 시에만 이용이 가능합니다.
      </Modal>
      
      {/* Overlay */}
      {isOpen && (
        <div 
          className="fixed inset-0 z-50 bg-black/40 backdrop-blur-[2px] transition-opacity duration-300" 
          onClick={onClose} 
        />
      )}

      {/* Side menu drawer */}
      <div
        className={`fixed inset-y-0 left-0 z-50 flex w-[280px] flex-col bg-white shadow-2xl transition-transform duration-500 cubic-bezier(0.4, 0, 0.2, 1) ${
          isOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* Header */}
        <div className="flex h-16 items-center justify-between border-b border-separator px-5">
          <Link to="/" onClick={onClose} className="flex items-center">
            <img
              src="/logo.svg"
              alt="My Fave"
              className="h-8 w-auto"
            />
          </Link>
          <button
            onClick={onClose}
            className="flex h-10 w-10 items-center justify-center rounded-full hover:bg-footer-bg active:scale-90 transition-all"
            aria-label="메뉴 닫기"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" className="text-dark-text" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        {/* Menu items */}
        <div className="flex-1 overflow-y-auto pt-4">
          <div className="space-y-1">
            {/* HOME */}
            <Link
              to="/"
              onClick={onClose}
              className="flex items-center px-6 py-4 transition-colors hover:bg-footer-bg active:bg-separator/30"
            >
              <span className="font-noto text-[18px] font-medium leading-[27px] text-dark-text">HOME</span>
            </Link>

            {/* SHOP */}
            <div className="border-b-[1.1px] border-separator">
              <button
                className="flex w-full items-center justify-between px-5 py-4 transition-colors hover:bg-footer-bg active:bg-separator/30"
                onClick={() => setShopOpen(!shopOpen)}
              >
                <span className="font-noto text-[18px] font-medium leading-[27px] text-dark-text">SHOP</span>
                <svg
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  className={`text-muted-text transition-transform duration-300 ${shopOpen ? 'rotate-180' : ''}`}
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <polyline points="6 9 12 15 18 9" />
                </svg>
              </button>
              {shopOpen && (
                <div className="bg-footer-bg/40 pb-4 pt-1">
                  {[
                    { label: '전체', path: '/shop' },
                    { label: '상의', path: '/shop?category=top' },
                    { label: '하의', path: '/shop?category=bottom' },
                    { label: '아우터', path: '/shop?category=outer' },
                    { label: '악세사리', path: '/shop?category=accessory' },
                  ].map((item) => (
                    <Link
                      key={item.label}
                      to={item.path}
                      onClick={onClose}
                      className="block px-10 py-3 font-noto text-[15px] font-medium text-dark-text/70 transition-colors hover:text-point active:translate-x-1"
                    >
                      {item.label}
                    </Link>
                  ))}
                </div>
              )}
            </div>

            {/* COMMUNITY */}
            <div className="border-b-[1.1px] border-separator">
              <button
                className="flex w-full items-center justify-between px-5 py-4 transition-colors hover:bg-footer-bg active:bg-separator/30"
                onClick={() => setCommunityOpen(!communityOpen)}
              >
                <span className="font-noto text-[18px] font-medium leading-[27px] text-dark-text">COMMUNITY</span>
                <svg
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  className={`text-muted-text transition-transform duration-300 ${communityOpen ? 'rotate-180' : ''}`}
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <polyline points="6 9 12 15 18 9" />
                </svg>
              </button>
              {communityOpen && (
                <div className="bg-footer-bg/40 pb-4 pt-1">
                  {[
                    { label: '마이 페이브 소개', path: '/about' },
                    { label: '공지사항', path: '/notice' },
                    { label: '자주 묻는 질문', path: '/faq' },
                  ].map((item) => (
                    <Link
                      key={item.label}
                      to={item.path}
                      onClick={onClose}
                      className="block px-10 py-3 font-noto text-[15px] font-medium text-dark-text/70 transition-colors hover:text-point active:translate-x-1"
                    >
                      {item.label}
                    </Link>
                  ))}
                  <a
                    href="https://www.instagram.com/daonmoood/"
                    target="_blank"
                    rel="noopener noreferrer"
                    onClick={onClose}
                    className="block px-10 py-3 font-noto text-[15px] font-medium text-dark-text/70 transition-colors hover:text-point active:translate-x-1"
                  >
                    1:1 문의
                  </a>
                </div>
              )}
            </div>

            {/* MY PAGE */}
            <button
              onClick={() => handleProtectedNavigation('/mypage')}
              className="flex w-full items-center border-b-[1.1px] border-separator px-5 py-4 transition-colors hover:bg-footer-bg"
            >
              <span className="font-noto text-[18px] font-medium leading-[27px] text-dark-text">MY PAGE</span>
            </button>

            {/* ORDER */}
            <button
              onClick={() => handleProtectedNavigation('/orders')}
              className="flex w-full items-center border-b-[1.1px] border-separator px-5 py-4 transition-colors hover:bg-footer-bg"
            >
              <span className="font-noto text-[18px] font-medium leading-[27px] text-dark-text">ORDER</span>
            </button>
          </div>
        </div>

        {/* Footer in SideMenu */}
        <div className="border-t border-separator p-6 bg-footer-bg/30" />
      </div>
    </>
  )
}
