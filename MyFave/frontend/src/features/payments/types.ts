import type { CartItem } from '@/features/cart/types'
import type { Coupon } from '@/features/coupons/types'
import type { Address } from '@/features/shipping/types'

export type PaymentMethod = '카드' | '계좌이체' | '카카오페이' | '네이버페이'

export interface CheckoutSession {
  items: CartItem[]
  address: Address | null
  appliedCoupon: Coupon | null
  paymentMethod: PaymentMethod | null
  orderType: 'DIRECT' | 'CART'
}

export type BackendPaymentMethod = 'CARD' | 'KAKAO_PAY' | 'NAVER_PAY' | 'TOSS_PAY'

export const PAYMENT_METHOD_MAP: Record<string, BackendPaymentMethod> = {
  '카드': 'CARD',
  '카카오페이': 'KAKAO_PAY',
  '네이버페이': 'NAVER_PAY',
  '토스페이': 'TOSS_PAY',
}

export interface PaymentPrepareRequest {
  orderId: number
  paymentMethod: BackendPaymentMethod
  discountCouponId?: number
  shippingCouponId?: number
}

export interface PaymentPrepareResponse {
  paymentId: number
  idempotencyKey: string
  storeId: string
  channelKey: string
  totalProductPrice: number
  deliveryFee: number
  discountPrice: number
  totalPaymentPrice: number
}

export interface PaymentConfirmRequest {
  paymentId: number
  pgTransactionId: string
}

export interface BackendPaymentResponse {
  paymentId: number
  paymentStatus: string
  totalPaymentPrice: number
  pgTransactionId: string
}
