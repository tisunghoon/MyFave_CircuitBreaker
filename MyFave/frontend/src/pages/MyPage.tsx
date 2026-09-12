import { useMemo } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { useLogout, useUser } from '@/features/auth/hooks'
import { useMyCoupons } from '@/features/coupons/hooks'
import { useOrdersQuery } from '@/features/orders/hooks'
import type { OrderSummaryApiResponse } from '@/features/orders/types'
import { UserIcon } from '@/shared/components/UserIcon'

type DashboardBucket = '입금확인' | '배송준비' | '배송중' | '배송완료'

// 백엔드 BackendOrderStatus + trackingNumber 유무를 4-bucket 대시보드 라벨로 환산.
// - PAID && !trackingNumber → 입금확인 (운영자가 아직 배송 정보 입력 안 함)
// - PAID && trackingNumber  → 배송준비 (운영자가 배송 정보 입력했지만 출고 전)
// - SHIPPING                → 배송중
// - DELIVERY_COMPLETED, PURCHASE_CONFIRMED → 배송완료
// - PENDING / CANCELLED / REFUNDED → 대시보드 집계 제외
function getDashboardBucket(order: OrderSummaryApiResponse): DashboardBucket | null {
  switch (order.orderStatus) {
    case 'PAID':
      return order.trackingNumber ? '배송준비' : '입금확인'
    case 'SHIPPING':
      return '배송중'
    case 'DELIVERY_COMPLETED':
    case 'PURCHASE_CONFIRMED':
      return '배송완료'
    default:
      return null
  }
}

const DASHBOARD_BUCKETS: DashboardBucket[] = ['입금확인', '배송준비', '배송중', '배송완료']

export function MyPage() {
  const navigate = useNavigate()
  const user = useUser()
  const logout = useLogout()
  // 로딩/에러 상태를 0 과 구분해서 표시 — 장애가 "주문 0건"으로 묻히지 않도록 (CR M13).
  const { data: availableCoupons = [], isLoading: isCouponsLoading, isError: isCouponsError } = useMyCoupons('AVAILABLE')
  const { data: ordersData, isLoading: isOrdersLoading, isError: isOrdersError } = useOrdersQuery()

  const orderCounts = useMemo(() => {
    const init: Record<DashboardBucket, number> = {
      입금확인: 0,
      배송준비: 0,
      배송중: 0,
      배송완료: 0,
    }
    const orders = ordersData?.content ?? []
    for (const order of orders) {
      const bucket = getDashboardBucket(order)
      if (bucket) init[bucket] += 1
    }
    return init
  }, [ordersData])

  // 로딩 중에는 "-", 에러 시 "!" 로 표기해 0 과 시각적으로 구분.
  const renderCount = (value: number, isLoading: boolean, isError: boolean): string => {
    if (isLoading) return '-'
    if (isError) return '!'
    return String(value)
  }

  const displayName = user ? `${user.nickname}님` : '비회원'
  const displayEmail = user?.email ?? ''

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="flex-1 bg-white min-h-0 pb-10">
      {/* 1. User Profile Section - Figma Node 99:1201 */}
      <div className="px-[19.99px] pt-[23.99px] pb-[19.99px]">
        <div className="flex items-center gap-[14px]">
          <UserIcon
            type="bear"
            variant={10}
            size={56}
            className="shadow-sm"
            profileImageUrl={user?.profileImageUrl}
          />
          <div className="flex flex-col gap-[1.99px]">
            <h2 className="font-noto text-[16px] font-medium leading-[24px] text-[#322927] tracking-tight">
              {displayName}
            </h2>
            <p className="font-noto text-[11px] font-normal leading-[16.5px] text-[#8B7E74]">
              {displayEmail}
            </p>
          </div>
        </div>
      </div>

      {/* 2. Order Status Container - Figma Node 37:4278 */}
      <div className="px-[19.99px] mb-[24px]">
        <div className="rounded-[12px] bg-footer-bg p-[15.99px] shadow-sm border border-separator/10">
          <div className="flex justify-between items-center h-[54.99px]">
            {DASHBOARD_BUCKETS.map((bucket) => (
              <Link
                key={bucket}
                to="/orders"
                className="flex flex-col items-center justify-between h-full w-[74.12px]"
              >
                <span className="font-noto text-[24px] font-medium leading-[36px] text-[#322927]">
                  {renderCount(orderCounts[bucket], isOrdersLoading, isOrdersError)}
                </span>
                <span className="font-noto text-[12px] font-normal leading-[18px] text-[#8B7E74]">
                  {bucket}
                </span>
              </Link>
            ))}
          </div>
        </div>
      </div>

      {/* 3. Section Divider - Figma Node 37:4300 (h:1.096px) */}
      <div className="h-[1.096px] w-full bg-separator" />

      {/* 4. Menu List Items - Figma Node 37:4301 ~ 4332 */}
      <div className="flex flex-col">
        {/* 주문조회 */}
        <Link 
          to="/orders" 
          className="flex h-[51px] items-center justify-between px-[19.99px] border-b-[1.096px] border-separator bg-white active:bg-gray-50 transition-colors"
        >
          <span className="font-noto text-[12px] font-medium text-[#322927]">주문조회</span>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#8B7E74" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="9 18 15 12 9 6" />
          </svg>
        </Link>

        {/* 쿠폰 */}
        <Link 
          to="/coupons" 
          className="flex h-[51px] items-center justify-between px-[19.99px] border-b-[1.096px] border-separator bg-white active:bg-gray-50 transition-colors"
        >
          <span className="font-noto text-[12px] font-medium text-[#322927]">쿠폰</span>
          <div className="flex items-center gap-1">
            <span className="font-noto text-[12px] font-medium text-chat-font">
              {renderCount(availableCoupons.length, isCouponsLoading, isCouponsError)}장
            </span>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#8B7E74" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </div>
        </Link>
        
        {/* 배송지 관리 */}
        <Link 
          to="/shipping-addresses" 
          className="flex h-[51px] items-center justify-between px-[19.99px] border-b-[1.096px] border-separator bg-white active:bg-gray-50 transition-colors"
        >
          <span className="font-noto text-[12px] font-medium text-[#322927]">배송지 관리</span>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#8B7E74" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="9 18 15 12 9 6" />
          </svg>
        </Link>

        {/* 로그아웃 */}
        <button 
          onClick={handleLogout}
          className="flex w-full h-[51px] items-center justify-between px-[19.99px] border-b-[1.096px] border-separator bg-white active:bg-gray-50 transition-colors"
        >
          <span className="font-noto text-[12px] font-medium text-[#322927]">로그아웃</span>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#8B7E74" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="9 18 15 12 9 6" />
          </svg>
        </button>
      </div>
    </div>
  )
}
