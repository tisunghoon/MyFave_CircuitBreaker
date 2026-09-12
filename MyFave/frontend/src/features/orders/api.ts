import { apiClient } from '@/shared/api/axios'
import type { OrderListApiResponse, OrderDetailApiResponse } from './types'

export interface OrderCreateRequest {
  orderType: 'DIRECT' | 'CART'
  productId?: number
  productIds?: number[]
  shippingAddressId: number
}

export interface BackendOrderResponse {
  orderId: number
  orderNumber: string
  orderType: string
  orderStatus: string
  createdAt: string
}

export const ordersApi = {
  create: async (data: OrderCreateRequest): Promise<BackendOrderResponse> => {
    const res = await apiClient.post<{ data: BackendOrderResponse }>('/orders', data)
    return res.data.data
  },

  getOrders: async (page = 0, size = 20): Promise<OrderListApiResponse> => {
    const res = await apiClient.get<{ data: OrderListApiResponse }>('/orders', {
      params: { page, size },
    })
    return res.data.data
  },

  getOrderDetail: async (orderId: number): Promise<OrderDetailApiResponse> => {
    const res = await apiClient.get<{ data: OrderDetailApiResponse }>(`/orders/${orderId}`)
    return res.data.data
  },

  confirmPurchase: async (orderId: number): Promise<void> => {
    await apiClient.patch(`/orders/${orderId}/confirm`)
  },
}
