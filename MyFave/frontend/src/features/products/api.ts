import { apiClient } from '@/shared/api/axios'
import type { ProductListApiResponse, ProductDetailApiResponse } from './types'

export const productsApi = {
  getList: async (page = 0, size = 50): Promise<ProductListApiResponse> => {
    const res = await apiClient.get<{ data: ProductListApiResponse }>('/products', {
      params: { page, size },
    })
    return res.data.data
  },
  getDetail: async (id: number): Promise<ProductDetailApiResponse> => {
    const res = await apiClient.get<{ data: ProductDetailApiResponse }>(`/products/${id}`)
    return res.data.data
  },
}
