import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { useIsAuthenticated } from '@/features/auth/hooks'
import { useCart } from '@/features/cart/hooks'
import { useCartStore } from '@/features/cart/store'
import { useCheckoutStore } from '@/features/payments/store'
import { Modal } from '@/shared/components/Modal'

export function CartPage() {
  const navigate = useNavigate()
  const items = useCart()
  const removeCartItem = useCartStore((s) => s.removeItem)
  const setCheckoutItems = useCheckoutStore((s) => s.setItems)
  const setOrderType = useCheckoutStore((s) => s.setOrderType)
  const isAuthenticated = useIsAuthenticated()
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false)

  const removeItem = (id: number) => {
    removeCartItem(id)
  }

  const handleCheckout = () => {
    if (items.length === 0) return

    if (!isAuthenticated) {
      setIsAuthModalOpen(true)
      return
    }

    setCheckoutItems(items)
    setOrderType('CART')
    navigate('/payment')
  }

  const subtotal = items.reduce((sum, item) => sum + item.price, 0)
  const shipping = 3000
  const total = subtotal + shipping

  return (
    <div className="flex-1 bg-white min-h-0 pb-32">
      <Modal
        isOpen={isAuthModalOpen}
        onClose={() => setIsAuthModalOpen(false)}
        buttonText="확인"
        onButtonClick={() => navigate('/login')}
      >
        회원들만 결제가 가능한 쇼핑몰입니다.<br />
        결제하시려면 회원가입 또는 로그인을 진행해주세요.
      </Modal>
      <div className="px-[19.99px] pt-8 space-y-[11.99px]">
        {items.length > 0 ? (
          items.map((item) => (
            <div
              key={item.id}
              className="flex w-full min-h-[114.19px] gap-[11.99px] rounded-[12px] border-[1.096px] border-separator bg-white p-[15.99px]"
            >
              <div className="h-[84px] w-[84px] flex-shrink-0 overflow-hidden rounded-[15px]">
                <img src={item.image} alt={item.title} className="h-full w-full object-cover" />
              </div>
              <div className="flex flex-1 flex-col justify-between">
                <div className="flex items-start justify-between">
                  <h3 className="font-noto text-[11px] font-bold leading-[15.13px] text-[#322927]">{item.title}</h3>
                  <button type="button" onClick={() => removeItem(item.id)} className="text-[#8B7E74] hover:text-red-500 transition-colors">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 6L6 18M6 6L18 18" strokeLinecap="round" strokeLinejoin="round" /></svg>
                  </button>
                </div>
                <div className="flex justify-end items-center">
                  <span className="font-noto text-[16px] font-bold leading-[24px] text-[#CF879B]">{item.price.toLocaleString()}원</span>
                </div>
              </div>
            </div>
          ))
        ) : (
          <div className="flex flex-col items-center justify-center py-32 space-y-8">
            <div className="text-center space-y-2">
              <h3 className="font-noto text-[18px] font-bold text-[#322927]">
                장바구니가 텅 비었어요
              </h3>
              <p className="font-noto text-[14px] text-[#8B7E74]">
                마음에 드는 상품을 담아보세요!
              </p>
            </div>

            <button
              type="button"
              onClick={() => navigate('/shop')}
              className="px-8 py-3 rounded-full bg-main-bg text-point font-noto text-[14px] font-bold border border-point/20 shadow-sm active:scale-[0.98] transition-all"
            >
              쇼핑하러 가기
            </button>
          </div>
        )}
      </div>

      {items.length > 0 && (
        <div className="mt-[22px] px-[19.99px]">
          <div className="rounded-[12px] bg-white p-[21.08px] space-y-[13.99px] border border-separator/30 shadow-sm">
            <div className="flex justify-between items-center">
              <span className="font-noto text-[14px] font-normal text-[#322927]">상품 금액</span>
              <span className="font-noto text-[14px] font-medium text-[#322927]">{subtotal.toLocaleString()}원</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="font-noto text-[14px] font-normal text-[#322927]">배송비</span>
              <span className="font-noto text-[14px] font-medium text-[#322927]">+{shipping.toLocaleString()}원</span>
            </div>
            <div className="pt-[12px] border-t border-separator/30">
              <div className="flex justify-between items-center">
                <span className="font-noto text-[18px] font-bold text-[#322927]">총 결제 예정 금액</span>
                <span className="font-noto text-[18px] font-bold text-point">{total.toLocaleString()}원</span>
              </div>
            </div>
          </div>
        </div>
      )}

      <div className="fixed bottom-0 left-1/2 z-40 w-full max-w-[376.04px] -translate-x-1/2 bg-white p-[19.99px] border-t border-separator shadow-figma-popup">
        <button
          type="button"
          onClick={handleCheckout}
          disabled={items.length === 0}
          className="flex h-[51.99px] w-full items-center justify-center rounded-[8px] bg-point font-noto text-[16px] font-bold text-white shadow-lg shadow-point/20 active:scale-[0.98] transition-all disabled:bg-gray-300"
        >
          결제하기
        </button>
      </div>
    </div>
  )
}
