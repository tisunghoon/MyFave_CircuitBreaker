import { apiClient } from '@/shared/api/axios'
import type { ApiResponse } from '@/shared/api/types'

import type { Coupon, CouponStatus } from './types'

export const couponsApi = {
  getMyCoupons: async (status?: CouponStatus): Promise<Coupon[]> => {
    const { data } = await apiClient.get<ApiResponse<Coupon[]>>('/coupons', {
      params: status ? { status } : undefined,
    })
    return data.data
  },
}
