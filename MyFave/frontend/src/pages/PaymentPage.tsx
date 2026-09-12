import PortOne from '@portone/browser-sdk/v2'
import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'

import { productsApi } from '@/features/products/api'

import { PopUp } from '@/shared/components/PopUp'

interface PortOnePaymentRequest {
  storeId: string
  channelKey: string
  paymentId: string
  orderName: string
  totalAmount: number
  currency: string
  payMethod: string
  easyPay?: { easyPayProvider: string }
  customer: {
    email: string
    fullName: string
    phoneNumber: string
  }
}

import { useUser } from '@/features/auth/hooks'
import { useCart } from '@/features/cart/hooks'
import { useCartStore } from '@/features/cart/store'
import { useCouponStore } from '@/features/coupons/store'
import { useCreateOrder } from '@/features/orders/hooks'
import { paymentsApi } from '@/features/payments/api'
import { useConfirmPayment, usePreparePayment } from '@/features/payments/hooks'
import { useCheckoutStore } from '@/features/payments/store'
import { PAYMENT_METHOD_MAP } from '@/features/payments/types'
import type { OrderCreateRequest } from '@/features/orders/api'
import { useShippingAddresses } from '@/features/shipping/hooks'
import { getDefaultAddress, useShippingStore } from '@/features/shipping/store'
import type { Address } from '@/features/shipping/types'

const SHIPPING_REQUESTS = [
  '문 앞에 두어주세요',
  '경비실에 맡겨주세요',
  '배송 전 미리 연락바랍니다',
  '직접 수령하겠습니다',
]

export function PaymentPage() {
  const navigate = useNavigate()
  const user = useUser()
  const checkoutItems = useCheckoutStore((s) => s.items)
  const setCheckoutItems = useCheckoutStore((s) => s.setItems)
  const orderType = useCheckoutStore((s) => s.orderType)
  const cartItems = useCart()
  const clearCart = useCartStore((s) => s.clear)
  const appliedCoupon = useCouponStore((s) => s.applied)
  const applyCoupon = useCouponStore((s) => s.applyCoupon)
  const addresses = useShippingStore((s) => s.addresses)

  const createOrder = useCreateOrder()
  const preparePayment = usePreparePayment()
  const confirmPayment = useConfirmPayment()

  const { data: backendAddresses } = useShippingAddresses()
  const defaultBackendAddress = backendAddresses?.find((a) => a.isDefault) ?? backendAddresses?.[0]

  const [selectedMethod, setSelectedMethod] = useState('카드')
  const [shippingRequest, setShippingRequest] = useState('')
  const [isRequestOpen, setIsRequestOpen] = useState(false)
  const [address, setAddress] = useState<Address | null>(null)
  const [isPopUpOpen, setIsPopUpOpen] = useState(false)
  const [popUpMessage, setPopUpMessage] = useState('')
  const [isPortOneOpen, setIsPortOneOpen] = useState(false)

  const showPopUp = (msg: string) => {
    setPopUpMessage(msg)
    setIsPopUpOpen(true)
  }

  const getPaymentErrorMessage = (error: unknown): string => {
    const errorCode = (error as { response?: { data?: { errorCode?: string } } })
      ?.response?.data?.errorCode
    switch (errorCode) {
      case 'PRODUCT_SOLD_OUT': return '상품이 품절되었습니다.'
      case 'PRODUCT_STOCK_INSUFFICIENT': return '재고가 부족합니다.'
      case 'PRODUCT_NOT_FOUND': return '상품을 찾을 수 없습니다.'
      case 'PAYMENT_FAILED': return '결제 서비스 오류가 발생했습니다. 잠시 후 다시 시도해주세요.'
      case 'PAYMENT_AMOUNT_MISMATCH': return '결제 금액이 일치하지 않아 자동 환불 처리되었습니다.'
      case 'PAYMENT_ALREADY_DONE': return '이미 처리된 결제입니다.'
      case 'PAYMENT_INVALID_STATUS': return '결제 상태가 올바르지 않습니다. 페이지를 새로고침 해주세요.'
      case 'PAYMENT_LOCK_CONFLICT': return '결제 처리 중 충돌이 발생했습니다. 다시 시도해주세요.'
      case 'ORDER_NOT_FOUND': return '주문을 찾을 수 없습니다.'
      case 'ORDER_INVALID_STATUS': return '주문 상태가 올바르지 않습니다.'
      default: return '결제 오류가 발생했습니다. 다시 시도해주세요.'
    }
  }

  useEffect(() => {
    if (checkoutItems.length === 0 && cartItems.length > 0) {
      setCheckoutItems(cartItems)
    }
  }, [checkoutItems.length, cartItems, setCheckoutItems])

  useEffect(() => {
    // 백엔드 주소가 있으면 반드시 백엔드 기준으로 설정 (로컬 스토어 stale 방지)
    if (defaultBackendAddress) {
      setAddress({
        id: String(defaultBackendAddress.shippingId),
        name: defaultBackendAddress.receiverName,
        phone: defaultBackendAddress.receiverPhone,
        address: defaultBackendAddress.address,
        detailAddress: defaultBackendAddress.addressDetail,
        zipcode: defaultBackendAddress.zipCode,
        request: defaultBackendAddress.deliveryRequest,
        isDefault: defaultBackendAddress.isDefault,
      })
      if (defaultBackendAddress.deliveryRequest) {
        setShippingRequest(defaultBackendAddress.deliveryRequest)
      }
    } else {
      // 백엔드 주소 없을 때만 로컬 스토어 폴백
      const defaultAddr = getDefaultAddress(addresses)
      if (defaultAddr) {
        setAddress(defaultAddr)
        if (defaultAddr.request) setShippingRequest(defaultAddr.request)
      }
    }
  }, [addresses, defaultBackendAddress])

  const greetingName = user ? `${user.nickname}님` : '비회원'

  const resolvedShippingId = address ? Number(address.id) : null

  const { data: backendProducts } = useQuery({
    queryKey: ['products'],
    queryFn: () => productsApi.getList(),
  })

  const backendPriceMap = useMemo(() => {
    const map: Record<number, number> = {}
    backendProducts?.content?.forEach((p) => { map[p.id] = p.price })
    return map
  }, [backendProducts])

  // 장바구니 stale 가격을 백엔드 최신 가격으로 동기화
  const syncedItems = useMemo(
    () => checkoutItems.map((item) => ({
      ...item,
      price: backendPriceMap[item.id] ?? item.price,
    })),
    [checkoutItems, backendPriceMap],
  )

  // 백엔드는 SHIPPING 쿠폰을 discountPrice 차감이 아니라 deliveryFee=0 으로 처리한다.
  // 프론트도 동일 규칙으로 계산해야 preparePayment 응답의 totalPaymentPrice 와 mismatch 가 발생하지 않음 (CR M16).
  const subtotal = syncedItems.reduce((sum, item) => sum + item.price, 0)
  const isShippingCoupon = appliedCoupon?.couponType === 'SHIPPING'
  const isDiscountCoupon = appliedCoupon?.couponType === 'DISCOUNT'
  const shippingFee = isShippingCoupon ? 0 : 3000
  const discount = isDiscountCoupon ? appliedCoupon!.discountPrice : 0
  const total = subtotal + shippingFee - discount

  const handlePayment = async (e: React.FormEvent) => {
    e.preventDefault()
    if (checkoutItems.length === 0 || !address || !resolvedShippingId) return

    const backendMethod = PAYMENT_METHOD_MAP[selectedMethod]
    if (!backendMethod) return

    // PortOne 취소/SDK 오류 분기에서 PENDING 결제를 cleanup 하기 위한 paymentId 저장 (CR M17).
    let pendingPaymentId: number | null = null

    // 백엔드 cancel 호출 — 실패해도 silent. 사용자에게는 별도 메시지 없음.
    const cleanupPendingPayment = async (reason: string) => {
      if (pendingPaymentId == null) return
      try {
        await paymentsApi.cancel(pendingPaymentId, { reason })
      } catch {
        // cleanup 실패는 사용자 흐름에 영향 없음 — 백엔드 웹훅이나 별도 정리 절차에 의존
      }
    }

    try {
      // 1. 주문 생성
      const orderPayload: OrderCreateRequest =
        orderType === 'CART'
          ? { orderType: 'CART', productIds: checkoutItems.map((i) => i.id), shippingAddressId: resolvedShippingId }
          : { orderType: 'DIRECT', productId: checkoutItems[0].id, shippingAddressId: resolvedShippingId }
      const order = await createOrder.mutateAsync(orderPayload)

      // 2. 결제 준비
      const prepareRes = await preparePayment.mutateAsync({
        orderId: order.orderId,
        paymentMethod: backendMethod,
        ...(appliedCoupon?.couponId && appliedCoupon.couponType === 'DISCOUNT' && { discountCouponId: appliedCoupon.couponId }),
        ...(appliedCoupon?.couponId && appliedCoupon.couponType === 'SHIPPING' && { shippingCouponId: appliedCoupon.couponId }),
      })

      // PortOne 실패/취소 시 cleanup 대상으로 저장.
      pendingPaymentId = prepareRes.paymentId

      // 금액 일치 검증 (백엔드 값끼리만 비교)
      const expectedTotal = prepareRes.totalProductPrice + prepareRes.deliveryFee - prepareRes.discountPrice
      if (prepareRes.totalPaymentPrice !== expectedTotal) {
        showPopUp('주문 금액이 변경되었습니다. 다시 시도해주세요.')
        await cleanupPendingPayment('AMOUNT_MISMATCH')
        return
      }

      // 3. PortOne 결제창 (열려있는 동안 버튼 비활성화)
      setIsPortOneOpen(true)
      const easyPayProvider = (
        {
          KAKAO_PAY: 'KAKAOPAY',
          NAVER_PAY: 'NAVERPAY',
          TOSS_PAY: 'TOSSPAY',
        } as Record<string, string>
      )[backendMethod]

      const portoneRes = await (PortOne.requestPayment as (req: PortOnePaymentRequest) => ReturnType<typeof PortOne.requestPayment>)({
        storeId: prepareRes.storeId,
        channelKey: prepareRes.channelKey,
        paymentId: prepareRes.idempotencyKey,
        orderName: `마이페이브 주문 ${checkoutItems.length}개`,
        totalAmount: prepareRes.totalPaymentPrice,
        currency: 'KRW',
        payMethod: backendMethod === 'CARD' ? 'CARD' : 'EASY_PAY',
        ...(easyPayProvider && { easyPay: { easyPayProvider } }),
        customer: {
          email: user?.email || 'buyer@myfave.com',
          fullName: user?.nickname || '구매자',
          phoneNumber: address.phone,
        },
      })

      if (!portoneRes || portoneRes.code) {
        const msg = portoneRes?.message ?? ''
        const isUserCancel = msg.includes('취소') || msg.toLowerCase().includes('cancel')
        showPopUp(isUserCancel ? '결제를 취소하셨습니다.' : `결제 실패: ${msg || '알 수 없는 오류'}`)
        await cleanupPendingPayment(isUserCancel ? 'USER_CANCEL' : `SDK_ERROR: ${msg}`)
        return
      }

      // 4. 결제 승인 (portoneRes.paymentId === prepareRes.idempotencyKey)
      await confirmPayment.mutateAsync({
        paymentId: prepareRes.paymentId,
        pgTransactionId: portoneRes.paymentId,
      })

      clearCart()
      applyCoupon(null)
      navigate('/order-success', {
        state: {
          orderNumber: order.orderNumber,
          items: syncedItems.map((item) => ({
            id: String(item.id),
            name: item.title,
            price: item.price.toLocaleString() + '원',
            image: item.image,
          })),
          paymentInfo: {
            productAmount: subtotal.toLocaleString() + '원',
            discountAmount: `-${discount.toLocaleString()}원`,
            shippingFee: (prepareRes.deliveryFee ?? shippingFee).toLocaleString() + '원',
            totalAmount: prepareRes.totalPaymentPrice.toLocaleString() + '원',
            paymentMethod: selectedMethod,
          },
          shipping: address
            ? {
                recipientName: address.name,
                phone: address.phone,
                address: address.address,
                detailAddress: address.detailAddress,
                request: shippingRequest || undefined,
              }
            : undefined,
        },
      })
    } catch (err) {
      // PortOne SDK가 resolve 대신 throw한 경우 (code 필드로 구분)
      const portoneErr = err as { code?: string; message?: string }
      if (portoneErr?.code) {
        const msg = portoneErr.message ?? ''
        const isUserCancel = msg.includes('취소') || msg.toLowerCase().includes('cancel')
        showPopUp(isUserCancel ? '결제를 취소하셨습니다.' : `결제 실패: ${msg || '알 수 없는 오류'}`)
        await cleanupPendingPayment(isUserCancel ? 'USER_CANCEL' : `SDK_THROW: ${msg}`)
        return
      }
      // confirmPayment 등 백엔드 호출에서 throw 한 경우 — 사용자에게 메시지 표시 후 PENDING 정리.
      showPopUp(getPaymentErrorMessage(err))
      await cleanupPendingPayment('CONFIRM_FAILED')
    } finally {
      // 어떤 종료 경로(성공/취소/예외)에서도 결제버튼 잠금 해제 보장
      setIsPortOneOpen(false)
    }
  }

  return (
    <div className="flex-1 bg-white min-h-0 pb-40 pt-8">
      <div className="px-[19.99px] pt-[17.01px] pb-[8px]">
        <h1 className="font-noto text-[15px] font-medium leading-[22px] text-[#322927]">{greetingName}</h1>
      </div>

      <div className="px-[19.99px]">
        <section className="mt-[16px] space-y-[12px]">
          <div className="flex items-center justify-between">
            <h2 className="font-noto text-[15px] font-bold text-[#322927]">배송지 정보</h2>
            {address && (
              <div className="flex gap-[6px] items-center">
                <div className="h-[20px] rounded-[5px] bg-[#EFE9E0] px-[8px] flex items-center justify-center">
                  <span className="font-noto text-[10px] font-medium text-[#949494] leading-none">기본 배송지</span>
                </div>
                <button
                  onClick={() => navigate('/shipping-addresses')}
                  className="h-[20px] rounded-[5px] bg-[#EFE9E0] px-[8px] flex items-center justify-center active:opacity-70 transition-opacity"
                >
                  <span className="font-noto text-[10px] font-medium text-[#949494] leading-none">배송지 변경</span>
                </button>
                <button
                  onClick={() => {
                    setAddress(null)
                    setShippingRequest('')
                  }}
                  className="ml-1 p-1 text-[#949494] hover:text-red-500 transition-colors"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18" />
                    <line x1="6" y1="6" x2="18" y2="18" />
                  </svg>
                </button>
              </div>
            )}
          </div>

          {address ? (
            <div className="rounded-[12px] border-[1.096px] border-[#F2EDEB] bg-white p-[20px] space-y-[10px] shadow-sm">
              <div className="space-y-[8px]">
                <p className="font-noto text-[13px] font-bold text-[#322927]">{address.name}</p>
                <p className="font-noto text-[12px] font-normal leading-[18.2px] text-[#322927]">
                  {address.address}
                  <br />
                  {address.detailAddress}
                </p>
                <p className="font-noto text-[12px] font-normal text-[#322927]">{address.phone}</p>
              </div>

              <div className="relative pt-[2px]">
                <button
                  onClick={() => setIsRequestOpen(!isRequestOpen)}
                  className="w-full h-[35px] flex items-center justify-between rounded-[5px] border border-[#F2EDEB] px-[12px] bg-white text-left transition-colors hover:border-point/30"
                >
                  <span className={`font-noto text-[12px] ${shippingRequest ? 'text-[#322927]' : 'text-[#949494]'}`}>
                    {shippingRequest || '배송 요청사항을 선택해주세요'}
                  </span>
                  <svg width="10" height="6" viewBox="0 0 10 6" fill="none" className={`text-[#949494] transition-transform ${isRequestOpen ? 'rotate-180' : ''}`}>
                    <path d="M1 1L5 5L9 1" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                </button>

                {isRequestOpen && (
                  <div className="absolute top-[38px] left-0 right-0 z-50 rounded-[5px] border border-[#F2EDEB] bg-white shadow-lg overflow-hidden">
                    {SHIPPING_REQUESTS.map((req) => (
                      <button
                        key={req}
                        onClick={() => {
                          setShippingRequest(req)
                          setIsRequestOpen(false)
                        }}
                        className="w-full px-[12px] py-[10px] text-left font-noto text-[12px] text-[#322927] hover:bg-main-bg transition-colors border-b border-separator/10 last:border-0"
                      >
                        {req}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            </div>
          ) : (
            <Link
              to="/add-shipping?from=/payment"
              className="w-full h-[32px] rounded-[12px] bg-point font-noto text-[12px] font-bold text-white shadow-md active:scale-[0.99] transition-all flex items-center justify-center gap-2"
            >
              배송지 등록하러가기
            </Link>
          )}
        </section>

        <section className="mt-[28px] space-y-[16px]">
          <h2 className="font-noto text-[15px] font-bold text-[#322927]">쿠폰 사용</h2>
          <button
            onClick={() => navigate('/coupons')}
            className="w-full h-[32px] rounded-[12px] bg-point font-noto text-[12px] font-bold text-white shadow-md active:scale-[0.99] transition-all"
          >
            {appliedCoupon
              ? `적용됨: ${appliedCoupon.couponType === 'SHIPPING' ? '배송비 무료' : `${appliedCoupon.discountPrice.toLocaleString()}원`}`
              : '쿠폰 사용'}
          </button>
        </section>

        <section className="mt-[32px] space-y-[16px]">
          <h2 className="font-noto text-[15px] font-bold text-[#322927]">주문 상품 {syncedItems.length}개</h2>
          <div className="space-y-[12px]">
            {syncedItems.map((item) => (
              <div key={item.id} className="flex w-full h-[114.19px] gap-[11.99px] rounded-[12px] border-[1.096px] border-[#F2EDEB] bg-white p-[15.99px] shadow-sm">
                <div className="h-[84px] w-[84px] flex-shrink-0 overflow-hidden rounded-[15px] shadow-sm">
                  <img src={item.image} alt={item.title} className="h-full w-full object-cover" />
                </div>
                <div className="flex flex-1 flex-col justify-between py-[2px]">
                  <h3 className="font-noto text-[11px] font-bold leading-[15.13px] text-[#322927] line-clamp-2">{item.title}</h3>
                  <div className="flex justify-end">
                    <span className="font-noto text-[16px] font-bold leading-[24px] text-[#CF879B]">{item.price.toLocaleString()}원</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="mt-[32px] space-y-[16px]">
          <h2 className="font-noto text-[15px] font-bold text-[#322927]">결제 수단</h2>
          <div className="grid grid-cols-2 gap-[11px]">
            {['카드', '카카오페이', '네이버페이', '토스페이'].map((label) => (
              <button key={label} onClick={() => setSelectedMethod(label)} className={`h-[42px] rounded-[5px] border font-noto text-[16px] font-medium transition-all ${selectedMethod === label ? 'border-point bg-main-bg text-point' : 'border-[#F2EDEB] bg-[#FAFAF8] text-[#949494]'}`}>{label}</button>
            ))}
          </div>
        </section>

        <section className="mt-[32px] space-y-[16px] pb-10">
          <h2 className="font-noto text-[15px] font-bold text-[#322927]">주문 금액</h2>
          <div className="rounded-[12px] border-[1.096px] border-[#F2EDEB] bg-white p-[21.08px] space-y-[14px] shadow-sm">
            <div className="flex justify-between items-center text-[14px]">
              <span className="font-noto font-normal text-[#8B7E74]">상품 금액</span>
              <span className="font-noto font-bold text-[#322927]">{subtotal.toLocaleString()}원</span>
            </div>
            <div className="flex justify-between items-center text-[14px]">
              <span className="font-noto font-normal text-[#8B7E74]">배송비</span>
              <span className="font-noto font-bold text-[#322927]">{shippingFee.toLocaleString()}원</span>
            </div>
            <div className="flex justify-between items-center text-[14px]">
              <span className="font-noto font-normal text-[#8B7E74]">할인 금액</span>
              <span className="font-noto font-bold text-point">-{discount.toLocaleString()}원</span>
            </div>
            <div className="pt-[14px] border-t-[1.096px] border-[#F2EDEB] flex justify-between items-center">
              <span className="font-noto text-[18px] font-bold text-[#322927]">최종 결제 금액</span>
              <span className="font-noto text-[20px] font-bold text-[#CF879B]">{total.toLocaleString()}원</span>
            </div>
          </div>
        </section>
      </div>

      <div className="fixed bottom-0 left-1/2 z-40 w-full max-w-[376.04px] -translate-x-1/2 bg-white p-[19.99px] border-t border-[#F2EDEB] shadow-figma-popup">
        <button
          onClick={handlePayment}
          disabled={isPortOneOpen || checkoutItems.length === 0 || !address || !resolvedShippingId || createOrder.isPending || preparePayment.isPending || confirmPayment.isPending}
          className="w-full h-[56px] rounded-[12px] bg-point flex flex-col items-center justify-center shadow-lg active:scale-[0.98] transition-all disabled:bg-gray-300"
        >
          {appliedCoupon && (
            <span className="font-noto text-[12px] text-white/60 line-through leading-none mb-[2px]">
              {(subtotal + shippingFee).toLocaleString()}원
            </span>
          )}
          <span className="font-noto text-[16px] font-black text-white uppercase tracking-tight">
            {total.toLocaleString()}원 결제하기
          </span>
        </button>
      </div>

      <PopUp isOpen={isPopUpOpen} message={popUpMessage} onClose={() => setIsPopUpOpen(false)} />
    </div>
  )
}
