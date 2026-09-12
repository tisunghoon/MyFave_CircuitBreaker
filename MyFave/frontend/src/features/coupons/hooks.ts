import { useQuery } from '@tanstack/react-query'

import { couponsApi } from './api'
import type { CouponStatus } from './types'

export function useMyCoupons(status?: CouponStatus) {
  return useQuery({
    queryKey: ['coupons', { status }],
    queryFn: () => couponsApi.getMyCoupons(status),
  })
}
