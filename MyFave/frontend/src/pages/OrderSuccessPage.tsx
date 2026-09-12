import { useEffect } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'

import type { OrderPaymentInfo, OrderShippingInfo } from '@/features/orders/types'

interface SuccessItem {
  id: string
  name: string
  price: string
  image: string
}

interface OrderSuccessState {
  orderNumber: string
  items: SuccessItem[]
  paymentInfo?: OrderPaymentInfo
  shipping?: OrderShippingInfo
}

function isOrderSuccessState(value: unknown): value is OrderSuccessState {
  if (!value || typeof value !== 'object') return false
  const v = value as Record<string, unknown>
  return typeof v.orderNumber === 'string' && Array.isArray(v.items)
}

export function OrderSuccessPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const rawState = location.state
  const state = isOrderSuccessState(rawState) ? rawState : null

  useEffect(() => {
    if (!state) navigate('/')
  }, [state, navigate])

  if (!state) return null

  const { orderNumber, items, paymentInfo, shipping } = state

  const orderDate = new Date().toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })

  return (
    <div className="flex-1 bg-white min-h-0 pb-32">
      {/* 1. Success Message */}
      <div className="flex flex-col items-center justify-center px-[19.99px] py-[40px] text-center">
        <div className="mb-[24px]">
          <div className="w-[80px] h-[80px] bg-main-bg rounded-full flex items-center justify-center shadow-sm border border-separator/5">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#FF95B3" strokeWidth="4" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="20 6 9 17 4 12" />
            </svg>
          </div>
        </div>
        <h1 className="font-noto text-[24px] font-bold text-point tracking-tight mb-[16px]">
          주문이 완료되었습니다
        </h1>
        <p className="font-noto text-[14px] leading-[22px] text-chat-font whitespace-pre-line mb-[24px]">
          {orderDate}{'\n'}
          주문번호 {orderNumber}
        </p>
        <div className="w-full bg-footer-bg rounded-[12px] p-[16px] border border-separator/10">
          <p className="font-noto text-[12px] leading-[20px] text-muted-text opacity-80 text-center">
            배송은 3~4일정도 걸리며<br />
            제주 및 도서 산간지역은 더 걸릴 수 있습니다.
          </p>
        </div>
      </div>

      {/* 2. Order Items Section */}
      <section className="px-[19.99px] py-[24px] space-y-[16px]">
        <h2 className="font-noto text-[15px] font-bold text-[#322927]">주문 상품 {items.length}개</h2>
        <div className="space-y-[12px]">
          {items.map((item) => (
            <div key={item.id} className="flex w-full h-[114.19px] gap-[11.99px] rounded-[12px] border-[1.096px] border-[#F2EDEB] bg-white p-[15.99px] shadow-sm">
              <div className="h-[84px] w-[84px] flex-shrink-0 overflow-hidden rounded-[15px] shadow-sm">
                <img src={item.image} alt={item.name} className="h-full w-full object-cover" />
              </div>
              <div className="flex flex-1 flex-col justify-between py-[2px]">
                <h3 className="font-noto text-[11px] font-bold leading-[15.13px] text-[#322927] line-clamp-2">{item.name}</h3>
                <div className="flex justify-end">
                  <span className="font-noto text-[16px] font-bold leading-[24px] text-[#CF879B]">{item.price}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* 3. Payment Detail Section */}
      {paymentInfo && (
        <section className="px-[19.99px] py-[24px] space-y-[16px]">
          <h2 className="font-noto text-[15px] font-bold text-[#322927]">결제 상세</h2>
          <div className="rounded-[12px] border border-[#F2EDEB] p-[20px] space-y-[14px] bg-white shadow-sm">
            <div className="flex justify-between items-center text-[14px]">
              <span className="font-noto text-[#8B7E74]">결제 수단</span>
              <span className="font-noto font-bold text-[#322927]">{paymentInfo.paymentMethod}</span>
            </div>
            <div className="pt-[14px] border-t border-separator/10 space-y-[10px]">
              <div className="flex justify-between items-center text-[14px]">
                <span className="font-noto text-[#8B7E74]">상품 금액</span>
                <span className="font-noto font-bold text-[#322927]">{paymentInfo.productAmount}</span>
              </div>
              <div className="flex justify-between items-center text-[14px]">
                <span className="font-noto text-[#8B7E74]">배송비</span>
                <span className="font-noto font-bold text-[#322927]">{paymentInfo.shippingFee}</span>
              </div>
              <div className="flex justify-between items-center text-[14px]">
                <span className="font-noto text-[#8B7E74]">쿠폰 할인</span>
                <span className="font-noto font-bold text-point">{paymentInfo.discountAmount}</span>
              </div>
            </div>
            <div className="pt-[14px] border-t-[1.096px] border-[#F2EDEB] flex justify-between items-center">
              <span className="font-noto text-[18px] font-bold text-[#322927]">총 결제 금액</span>
              <span className="font-noto text-[20px] font-bold text-point">{paymentInfo.totalAmount}</span>
            </div>
          </div>
        </section>
      )}

      {/* 4. Shipping Info Section */}
      {shipping && (
        <section className="px-[19.99px] py-[24px] space-y-[16px]">
          <h2 className="font-noto text-[15px] font-bold text-[#322927]">배송 정보</h2>
          <div className="rounded-[12px] border border-[#F2EDEB] p-[20px] space-y-[8px] bg-white shadow-sm">
            <p className="font-noto text-[14px] font-bold text-[#322927]">
              {shipping.recipientName}
              <span className="font-normal text-[12px] text-muted-text ml-2">{shipping.phone}</span>
            </p>
            <p className="font-noto text-[12px] font-normal leading-[18.2px] text-[#322927]">
              {shipping.address}
              {shipping.detailAddress ? <><br />{shipping.detailAddress}</> : null}
            </p>
            <div className="pt-2 border-t border-separator/10">
              <p className="font-noto text-[11px] text-[#8B7E74]">배송 요청사항: {shipping.request || '없음'}</p>
            </div>
          </div>
        </section>
      )}

      {/* 5. Bottom Action Buttons */}
      <div className="px-[19.99px] py-[48px] space-y-[12px]">
        <Link
          to="/orders"
          className="flex h-[56px] w-full items-center justify-center rounded-[12px] bg-point font-noto text-[16px] font-bold text-white shadow-lg shadow-point/20 active:scale-[0.98] transition-all text-center"
        >
          주문 조회하기
        </Link>
        <Link
          to="/"
          className="flex h-[56px] w-full items-center justify-center rounded-[12px] border border-[#F2EDEB] bg-white font-noto text-[16px] font-bold text-[#322927] active:scale-[0.98] transition-all text-center"
        >
          계속 쇼핑하기
        </Link>
      </div>
    </div>
  )
}
