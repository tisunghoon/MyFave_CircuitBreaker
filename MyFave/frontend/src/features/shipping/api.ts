import { apiClient } from '@/shared/api/axios'
import type { ApiResponse } from '@/shared/api/types'

import type { TrackingResponse } from './types'

export interface BackendShippingAddress {
  shippingId: number
  receiverName: string
  receiverPhone: string
  address: string
  addressDetail: string
  zipCode: string
  deliveryRequest: string
  isDefault: boolean
}

export interface ShippingAddressCreateRequest {
  receiverName: string
  receiverPhone: string
  address: string
  addressDetail?: string
  zipCode: string
  deliveryRequest?: string
  isDefault?: boolean
}

export interface ShippingAddressUpdateRequest {
  receiverName: string
  receiverPhone: string
  address: string
  addressDetail?: string
  zipCode: string
  deliveryRequest?: string
  isDefault?: boolean
}

export const shippingApi = {
  getAddresses: async (): Promise<BackendShippingAddress[]> => {
    const res = await apiClient.get<{ data: BackendShippingAddress[] }>('/shipping')
    return res.data.data
  },
  createAddress: async (data: ShippingAddressCreateRequest): Promise<BackendShippingAddress> => {
    const res = await apiClient.post<{ data: BackendShippingAddress }>('/shipping', data)
    return res.data.data
  },
  updateAddress: async (shippingId: number, data: ShippingAddressUpdateRequest): Promise<BackendShippingAddress> => {
    const res = await apiClient.put<{ data: BackendShippingAddress }>(`/shipping/${shippingId}`, data)
    return res.data.data
  },
  deleteAddress: async (shippingId: number): Promise<void> => {
    await apiClient.delete(`/shipping/${shippingId}`)
  },
  setDefaultAddress: async (shippingId: number): Promise<void> => {
    await apiClient.patch(`/shipping/${shippingId}/default`)
  },
  getTracking: async (orderId: number): Promise<TrackingResponse> => {
    const { data } = await apiClient.get<ApiResponse<TrackingResponse>>(`/orders/${orderId}/tracking`)
    return data.data
  },
}
