import { useMutation, useQueryClient } from '@tanstack/react-query'
import { paymentsApi } from './api'

export function usePreparePayment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: paymentsApi.prepare,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] })
    },
  })
}

export function useConfirmPayment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: paymentsApi.confirm,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      queryClient.invalidateQueries({ queryKey: ['payments'] })
    },
  })
}
