import { create } from 'zustand'
import { persist } from 'zustand/middleware'

import type { Coupon } from './types'

interface CouponState {
  applied: Coupon | null
  applyCoupon: (coupon: Coupon | null) => void
}

export const useCouponStore = create<CouponState>()(
  persist(
    (set) => ({
      applied: null,
      applyCoupon: (coupon) => set({ applied: coupon }),
    }),
    { name: 'myfave-coupons' },
  ),
)
