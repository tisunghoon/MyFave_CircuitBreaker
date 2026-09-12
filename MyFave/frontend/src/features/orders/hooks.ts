import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ordersApi } from './api'

export function useOrdersQuery() {
  return useQuery({
    queryKey: ['orders'],
    queryFn: () => ordersApi.getOrders(),
    // 마이페이지 주문 상태 대시보드가 백엔드 배송 정보 등록 결과를 자동 반영하도록
    // 30 초 폴링 + 탭 복귀 시 재검증. 운영자가 배송 정보 입력하면 30 초 이내 사용자 화면에 반영.
    refetchInterval: 30_000,
    refetchOnWindowFocus: true,
  })
}

export function useOrderDetailQuery(orderId: number | undefined) {
  return useQuery({
    queryKey: ['order', orderId],
    queryFn: () => ordersApi.getOrderDetail(orderId!),
    enabled: Number.isFinite(orderId),
  })
}

export function useCreateOrder() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ordersApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] })
    },
  })
}

export function useConfirmPurchase() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (orderId: number) => ordersApi.confirmPurchase(orderId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] })
    },
  })
}
