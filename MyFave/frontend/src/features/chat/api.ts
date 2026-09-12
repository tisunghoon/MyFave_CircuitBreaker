import { apiClient } from '@/shared/api/axios'
import type { ApiResponse } from '@/shared/api/types'
import type { ChatRoomInfo, ChatHistoryResponse, ChatPreviewResponse } from './types'

export const chatApi = {
  getRoomInfo: async (): Promise<ChatRoomInfo> => {
    const { data } = await apiClient.get<ApiResponse<ChatRoomInfo>>('/chat-room')
    return data.data
  },

  getMessageHistory: async (size = 50, before?: string): Promise<ChatHistoryResponse> => {
    const { data } = await apiClient.get<ApiResponse<ChatHistoryResponse>>('/chat-room/messages', {
      params: { size, ...(before !== undefined ? { before } : {}) },
    })
    return data.data
  },

  getPreview: async (size = 5): Promise<ChatPreviewResponse> => {
    const { data } = await apiClient.get<ApiResponse<ChatPreviewResponse>>('/chat-room/preview', {
      params: { size },
    })
    return data.data
  },
}
