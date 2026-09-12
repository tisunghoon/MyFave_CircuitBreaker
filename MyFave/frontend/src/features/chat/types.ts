export interface ChatRoomInfo {
  id: number
  isActive: boolean
  participantCount: number
  createdAt: string
}

export interface ChatHistoryMessage {
  messageId: string
  senderId: number
  senderNickname: string
  isInfluencer: boolean
  content: string
  createdAt: string
}

export interface ChatHistoryResponse {
  messages: ChatHistoryMessage[]
  hasMore: boolean
}

export interface ChatPreviewMessage {
  senderNickname: string
  content: string
  createdAt: string
}

export interface ChatPreviewResponse {
  isActive: boolean
  participantCount: number
  recentMessages: ChatPreviewMessage[]
}
