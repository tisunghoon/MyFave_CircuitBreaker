import { create } from 'zustand'

import type { CartItem } from '@/features/cart/types'
import type { Coupon } from '@/features/coupons/types'
import type { Address } from '@/features/shipping/types'

import type { CheckoutSession, PaymentMethod } from './types'

interface CheckoutState extends CheckoutSession {
  setItems: (items: CartItem[]) => void
  setAddress: (address: Address | null) => void
  setCoupon: (coupon: Coupon | null) => void
  setPaymentMethod: (method: PaymentMethod | null) => void
  setOrderType: (orderType: 'DIRECT' | 'CART') => void
  reset: () => void
}

const initialSession: CheckoutSession = {
  items: [],
  address: null,
  appliedCoupon: null,
  paymentMethod: null,
  orderType: 'DIRECT',
}

export const useCheckoutStore = create<CheckoutState>()((set) => ({
  ...initialSession,
  setItems: (items) => set({ items }),
  setAddress: (address) => set({ address }),
  setCoupon: (appliedCoupon) => set({ appliedCoupon }),
  setPaymentMethod: (paymentMethod) => set({ paymentMethod }),
  setOrderType: (orderType) => set({ orderType }),
  reset: () => set(initialSession),
}))
