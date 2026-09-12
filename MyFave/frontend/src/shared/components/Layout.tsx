import { Outlet, useLocation } from 'react-router-dom'

import { Footer } from '@/shared/components/Footer'
import { Header } from '@/shared/components/Header'

export function Layout() {
  const location = useLocation()

  const getHeaderProps = () => {
    const path = location.pathname
    
    if (path === '/payment') return { title: '주문서', showBackButton: true }
    if (path === '/cart') return { title: '장바구니', showBackButton: true }
    if (path === '/add-shipping') return { title: '배송지 정보', showBackButton: true }
    if (path === '/coupons') return { title: '쿠폰', showBackButton: true }
    if (path === '/order-success') return { title: '주문완료', showBackButton: false }
    if (path === '/orders' || path.startsWith('/orders/')) return { title: '주문 조회', showBackButton: true }
    if (path.startsWith('/shipping-status/')) return { title: '배송 현황', showBackButton: true }
    if (path === '/about') return { title: '마이페이브 소개', showBackButton: true }
    if (path === '/notice') return { title: '공지사항', showBackButton: true }
    if (path === '/faq') return { title: '자주 묻는 질문', showBackButton: true }
    if (path === '/shipping-addresses') return { title: '배송지 관리', showBackButton: true }
    if (path === '/shipping') return { title: '배송/주문 안내', showBackButton: true }
    if (path === '/business') return { title: '사업자 정보', showBackButton: true }
    if (path === '/terms') return { title: '이용약관', showBackButton: true }
    if (path === '/privacy') return { title: '개인정보처리방침', showBackButton: true }

    return {} // Default logo header
  }

  return (
    <div className="min-h-screen w-full bg-[#E2AFAF] flex justify-center overflow-x-hidden">
      <div className="w-full max-w-[376.04px] min-h-screen bg-white shadow-figma-app flex flex-col relative">
        <Header {...getHeaderProps()} />
        <main className="flex-1 flex flex-col overflow-x-hidden">
          <Outlet />
        </main>
        <Footer />
      </div>
    </div>
  )
}
