export type CouponType = 'DISCOUNT' | 'SHIPPING'
export type CouponStatus = 'AVAILABLE' | 'USED' | 'EXPIRED'

export interface Coupon {
  couponId: number
  couponName: string
  couponType: CouponType
  discountPrice: number
  status: CouponStatus
  expiredAt: string
  createdAt: string
}
