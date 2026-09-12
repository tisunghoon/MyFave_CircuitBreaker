import axios from 'axios'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { shippingApi } from './api'

export const SHIPPING_QUERY_KEY = ['shipping-addresses'] as const

export function useShippingAddresses() {
  return useQuery({
    queryKey: SHIPPING_QUERY_KEY,
    queryFn: shippingApi.getAddresses,
  })
}

export function useDeleteShippingAddress() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (shippingId: number) => shippingApi.deleteAddress(shippingId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: SHIPPING_QUERY_KEY }),
  })
}

export function useSetDefaultShippingAddress() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (shippingId: number) => shippingApi.setDefaultAddress(shippingId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: SHIPPING_QUERY_KEY }),
  })
}

// 재시도 중단 대상 errorCode — 운송장 미등록/택배사 API 오류는 재시도해도 즉시 회복되지 않음.
const TRACKING_NON_RETRIABLE_CODES = new Set(['TRACKING_NOT_REGISTERED', 'TRACKING_API_ERROR'])

export function useTracking(orderId: number | undefined) {
  return useQuery({
    queryKey: ['tracking', orderId],
    queryFn: () => shippingApi.getTracking(orderId!),
    enabled: !!orderId,
    refetchInterval: 30_000,
    retry: (failureCount, error) => {
      // 백엔드 errorCode 기준으로 분기 — 프로젝트 가이드라인 (CR M6).
      if (axios.isAxiosError(error)) {
        const code = error.response?.data?.errorCode as string | undefined
        if (code && TRACKING_NON_RETRIABLE_CODES.has(code)) return false
      }
      return failureCount < 2
    },
  })
}
