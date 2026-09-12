import { apiClient } from '@/shared/api/axios'
import type {
  PaymentPrepareRequest,
  PaymentPrepareResponse,
  PaymentConfirmRequest,
  BackendPaymentResponse,
} from './types'

export interface PaymentCancelRequest {
  reason: string
  refundAmount?: number
}

export const paymentsApi = {
  prepare: async (data: PaymentPrepareRequest): Promise<PaymentPrepareResponse> => {
    const res = await apiClient.post<{ data: PaymentPrepareResponse }>('/payments/prepare', data)
    return res.data.data
  },
  confirm: async (data: PaymentConfirmRequest): Promise<BackendPaymentResponse> => {
    const res = await apiClient.post<{ data: BackendPaymentResponse }>('/payments/confirm', data)
    return res.data.data
  },
  // PortOne 사용자 취소/SDK 오류 시 PENDING 상태로 남은 결제를 정리해 재시도가 PAYMENT_ALREADY_DONE 으로 막히지 않게 한다 (CR M17).
  cancel: async (paymentId: number, data: PaymentCancelRequest): Promise<BackendPaymentResponse> => {
    const res = await apiClient.post<{ data: BackendPaymentResponse }>(`/payments/${paymentId}/cancel`, data)
    return res.data.data
  },
}
