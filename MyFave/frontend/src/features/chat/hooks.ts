import { useQuery } from '@tanstack/react-query'
import { chatApi } from './api'

export function useChatRoomInfo() {
  return useQuery({
    queryKey: ['chat', 'room'],
    queryFn: chatApi.getRoomInfo,
    staleTime: 10_000,
    refetchInterval: 5_000,
    refetchIntervalInBackground: true,
    refetchOnWindowFocus: true,
    retry: 1,
  })
}

export function useChatMessageHistory(roomId: number | undefined, size = 50) {
  return useQuery({
    queryKey: ['chatHistory', { roomId, size }],
    queryFn: () => chatApi.getMessageHistory(size),
    enabled: roomId != null,
    staleTime: Infinity,
    refetchOnMount: 'always',
    retry: false,
  })
}

export function useChatPreview(size = 5) {
  return useQuery({
    queryKey: ['chat', 'preview', size],
    queryFn: () => chatApi.getPreview(size),
    staleTime: 30_000,
    retry: false,
  })
}
