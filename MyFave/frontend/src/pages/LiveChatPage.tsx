// frontend/src/pages/LiveChatPage.tsx
import { useState, useEffect, useRef } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { UserIcon } from '@/shared/components/UserIcon'
import { useChatRoomInfo, useChatMessageHistory } from '@/features/chat/hooks'
import { useAuthStore } from '@/features/auth/store'
import type { ChatHistoryMessage } from '@/features/chat/types'
import { getChatLifecycleState, getChatOpenAt, getCountdownSeconds } from '@/shared/utils/saleSchedule'

const THROTTLE_MS = 3000
const BEAR_VARIANTS = [1, 3, 5, 6, 7, 10] as const

function getVariantFromNickname(nickname: string): number {
  let hash = 0
  for (let i = 0; i < nickname.length; i++) {
    hash = (hash + nickname.charCodeAt(i)) % BEAR_VARIANTS.length
  }
  return BEAR_VARIANTS[hash]
}

interface Message {
  id: string | number
  user: string
  text: string
  avatarType: 'bear' | 'human' | 'seller'
  avatarVariant: number
  isOfficial?: boolean
  timestamp: string
}

function historyToMessage(msg: ChatHistoryMessage): Message {
  return {
    id: msg.messageId,
    user: msg.senderNickname,
    text: msg.content,
    avatarType: msg.isInfluencer ? 'seller' : 'bear',
    avatarVariant: msg.isInfluencer ? 1 : getVariantFromNickname(msg.senderNickname),
    isOfficial: msg.isInfluencer,
    timestamp: new Date(msg.createdAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' }),
  }
}

function InactiveRoomScreen() {
  return (
    <div className="flex flex-1 flex-col items-center justify-center bg-white">
      <div className="flex flex-col items-center gap-4 px-8 text-center">
        <p className="font-noto text-[16px] font-bold text-dark-text">채팅방이 곧 활성화 됩니다</p>
        <p className="font-noto text-[12px] text-muted-text">오픈 30분 전에 채팅방이 열립니다</p>
      </div>
    </div>
  )
}

// 판매 시작 30분 전 이전 — 잠금 화면.
function BeforeOpenScreen({ countdownSeconds }: { countdownSeconds: number }) {
  // 오픈까지 남은 시간 (= 판매 시작 30분 전까지 남은 시간 = countdownSeconds - 30분).
  const chatOpenAt = getChatOpenAt()
  const openLabel = chatOpenAt.toLocaleString('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
  const days = Math.floor(countdownSeconds / 86400)
  const hms = countdownSeconds % 86400
  const h = Math.floor(hms / 3600)
  const m = Math.floor((hms % 3600) / 60)
  const s = hms % 60
  const remainingLabel =
    days > 0
      ? `${days}일 ${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
      : `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`

  return (
    <div className="flex flex-1 flex-col items-center justify-center bg-white">
      <div className="flex flex-col items-center gap-3 px-8 text-center">
        <div className="mb-2 flex h-[64px] w-[64px] items-center justify-center rounded-full bg-main-bg">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#FF95B3" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="9" />
            <polyline points="12 7 12 12 15 14" />
          </svg>
        </div>
        <p className="font-noto text-[16px] font-bold text-dark-text">라이브 톡이 아직 열리지 않았어요</p>
        <p className="font-noto text-[12px] text-muted-text">
          판매 시작 30분 전에 자동으로 열려요
          <br />
          오픈 예정 — {openLabel}
        </p>
        <p className="mt-2 font-lexend text-[14px] font-semibold text-point tracking-tight">
          판매 시작까지 {remainingLabel}
        </p>
      </div>
    </div>
  )
}

// 운영자가 ROOM_CLOSED 로 닫았거나 판매 시작 이후 — 종료 화면.
function ClosedRoomScreen() {
  return (
    <div className="flex flex-1 flex-col items-center justify-center bg-white">
      <div className="flex flex-col items-center gap-3 px-8 text-center">
        <div className="mb-2 flex h-[64px] w-[64px] items-center justify-center rounded-full bg-[#F2EDEB]">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#8B7E74" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="3" y="6" width="18" height="14" rx="2" />
            <path d="M3 10h18" />
          </svg>
        </div>
        <p className="font-noto text-[16px] font-bold text-dark-text">라이브 톡이 종료되었습니다</p>
        <p className="font-noto text-[12px] text-muted-text">곧 본 판매가 시작됩니다. 잠시만 기다려주세요!</p>
      </div>
    </div>
  )
}

export function LiveChatPage() {
  const { data: roomInfo, isLoading: isRoomLoading, isError: isRoomError } = useChatRoomInfo()
  const { data: historyData } = useChatMessageHistory(roomInfo?.id)

  const [messages, setMessages] = useState<Message[]>([])
  const [inputText, setInputText] = useState('')
  const [isNoticeOpen, setIsNoticeOpen] = useState(true)
  const [participantCount, setParticipantCount] = useState(0)
  const [isConnected, setIsConnected] = useState(false)
  const [isCooldown, setIsCooldown] = useState(false)
  const [isRoomClosed, setIsRoomClosed] = useState(false)
  const [sendError, setSendError] = useState<string | null>(null)
  const scrollRef = useRef<HTMLDivElement>(null)
  const stompClient = useRef<Client | null>(null)
  const lastSendTimeRef = useRef<number>(0)
  const historyInitializedRef = useRef(false)

  useEffect(() => {
    if (roomInfo?.participantCount != null) {
      // 서버 query 값을 local state로 초기 동기화 (이후 WS의 PARTICIPANT_COUNT 메시지가 갱신)
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setParticipantCount(roomInfo.participantCount)
    }
  }, [roomInfo?.participantCount])

  useEffect(() => {
    if (!historyData?.messages.length || historyInitializedRef.current) return
    historyInitializedRef.current = true
    // 과거 채팅 이력을 한 번만 초기 주입 (이후 WS의 NEW_MESSAGE가 누적)
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setMessages(historyData.messages.map(historyToMessage))
  }, [historyData])

  useEffect(() => {
    if (!roomInfo?.isActive || !roomInfo?.id) return

    const wsBaseUrl = import.meta.env?.VITE_WS_BASE_URL
    if (!wsBaseUrl) return

    const roomId = roomInfo.id
    const httpUrl = wsBaseUrl.replace('ws://', 'http://').replace('wss://', 'https://')

    try {
      const client = new Client({
        webSocketFactory: () => new SockJS(httpUrl),
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      })

      client.beforeConnect = () => {
        const token = useAuthStore.getState().accessToken ?? ''
        client.connectHeaders = { Authorization: `Bearer ${token}` }
      }

      client.onConnect = () => {
        setIsConnected(true)
        client.subscribe(`/topic/chat/${roomId}`, (message) => {
          try {
            const data = JSON.parse(message.body)

            if (data.type === 'PARTICIPANT_COUNT') {
              setParticipantCount(data.payload.count)
            }

            if (data.type === 'NEW_MESSAGE') {
              const payload = data.payload
              const isOfficial = payload.nickname?.includes('공식') ?? false
              const newMessage: Message = {
                id: payload.messageId ?? Date.now(),
                user: payload.nickname,
                text: payload.content,
                avatarType: isOfficial ? 'seller' : 'bear',
                avatarVariant: isOfficial ? 1 : getVariantFromNickname(payload.nickname),
                isOfficial,
                timestamp: new Date(payload.sentAt).toLocaleTimeString('ko-KR', {
                  hour: '2-digit',
                  minute: '2-digit',
                }),
              }
              setMessages((prev) => [...prev, newMessage])
            }

            if (data.type === 'ROOM_CLOSED') {
              setIsRoomClosed(true)
              setIsConnected(false)
              client.reconnectDelay = 0
              client.deactivate()
            }

            if (data.type === 'RATE_LIMIT') {
              setIsCooldown(true)
              setTimeout(() => setIsCooldown(false), THROTTLE_MS)
            }
          } catch {
            // ignore malformed WS frames
          }
        })
      }

      client.onDisconnect = () => setIsConnected(false)
      client.onStompError = () => setIsConnected(false)

      client.activate()
      stompClient.current = client
    } catch {
      // WS unavailable — fallback to local-only mode
    }

    return () => {
      stompClient.current?.deactivate()
    }
  }, [roomInfo?.id, roomInfo?.isActive])

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
    }
  }, [messages])

  // React Compiler가 자동 메모이즈하므로 useCallback 제거 (Compiler의 deps 추론과 manual deps mismatch 회피)
  const handleSend = (e: React.FormEvent) => {
    e.preventDefault()
    if (!inputText.trim() || isRoomClosed) return

    const now = Date.now()
    if (now - lastSendTimeRef.current < THROTTLE_MS) return
    lastSendTimeRef.current = now

    if (stompClient.current?.connected && roomInfo?.id) {
      stompClient.current.publish({
        destination: `/app/chat/${roomInfo.id}`,
        body: JSON.stringify({ type: 'SEND_MESSAGE', payload: { content: inputText } }),
      })
      setSendError(null)
      setInputText('')
      setIsCooldown(true)
      setTimeout(() => setIsCooldown(false), THROTTLE_MS)
    } else {
      setSendError('서버와 연결이 끊어졌습니다. 잠시 후 다시 시도해주세요.')
    }
  }

  // 시간 기반 라이프사이클 게이트를 위해 1초마다 now 갱신.
  const [now, setNow] = useState(() => new Date())
  useEffect(() => {
    const tick = setInterval(() => setNow(new Date()), 1000)
    return () => clearInterval(tick)
  }, [])

  if (isRoomLoading) {
    return (
      <div className="flex flex-1 items-center justify-center bg-white">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-point border-t-transparent" />
      </div>
    )
  }

  // 3-state 게이트:
  // 1) 운영자가 ROOM_CLOSED 로 종료 → CLOSED
  // 2) 현재 시각이 채팅 오픈 시각(판매시작 30분 전) 이전 → BEFORE_OPEN
  // 3) 백엔드가 isActive=false 또는 에러 → InactiveRoomScreen (이전 폴백)
  // 4) 그 외 → OPEN
  const lifecycle = getChatLifecycleState({ now, isAdminClosed: isRoomClosed })
  if (lifecycle === 'CLOSED') return <ClosedRoomScreen />
  if (lifecycle === 'BEFORE_OPEN') return <BeforeOpenScreen countdownSeconds={getCountdownSeconds(now)} />

  if (isRoomError || !roomInfo?.isActive) {
    return <InactiveRoomScreen />
  }

  return (
    <div className="relative flex flex-1 flex-col bg-white overflow-hidden min-h-0">
      {/* 0. Background Watermark */}
      <div className="absolute inset-0 z-0 flex items-center justify-center pointer-events-none overflow-hidden opacity-[0.20]">
        <img src="/logo.svg" alt="My Fave Watermark" className="w-[50%] h-auto grayscale" style={{ imageRendering: 'auto' }} />
      </div>

      {/* 1. Header Notice Card */}
      <div className="relative z-20 px-[20px] pt-[32px] pb-[16px]">
        <div className={`relative rounded-[24px] bg-sub1 transition-all duration-300 ${isNoticeOpen ? 'p-[24px]' : 'py-[14px] px-[24px]'}`}>
          <div className={`overflow-hidden transition-all duration-300 ${isNoticeOpen ? 'max-h-[100px] opacity-100' : 'max-h-[20px] opacity-100'}`}>
            <p className={`text-center font-noto text-[12px] font-bold leading-[18.2px] text-[#000000] tracking-tight ${!isNoticeOpen && 'truncate px-4'}`}>
              My Fave 판매 시작 30분전 라이브 채팅
              {isNoticeOpen && (
                <>
                  <br />
                  <span className="font-normal opacity-90">
                    오픈 30분 전, 지금이 찬스!
                    <br />
                    셀러에게 직접 물어보고 쇼핑 준비 완료하세요🎀
                  </span>
                </>
              )}
            </p>
          </div>
          <button
            onClick={() => setIsNoticeOpen(!isNoticeOpen)}
            className="absolute right-4 top-1/2 -translate-y-1/2 flex h-8 w-8 items-center justify-center rounded-full hover:bg-black/5 active:scale-90 transition-all"
            aria-label={isNoticeOpen ? '공지 접기' : '공지 펴기'}
          >
            <svg
              width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"
              className={`transition-transform duration-300 ${isNoticeOpen ? 'rotate-180' : 'rotate-0'}`}
            >
              <polyline points="6 9 12 15 18 9" />
            </svg>
          </button>
        </div>
      </div>

      {/* 2. Participant Badge + WS 연결 상태 */}
      <div className="relative z-20 flex items-center justify-center gap-[8px] pb-[24px]">
        <div className="inline-flex items-center justify-center rounded-[10px] bg-sub1 px-[12px] py-[4px] border border-black/5 shadow-inner">
          <span className="font-noto text-[11px] font-medium text-[#FF6B6B]">
            {participantCount.toLocaleString()}명이 참여중입니다
          </span>
        </div>
        <div className="inline-flex items-center gap-[4px] rounded-[10px] bg-sub1 px-[10px] py-[4px] border border-black/5">
          <span className={`h-[6px] w-[6px] rounded-full ${isConnected ? 'bg-green-400 animate-pulse' : 'bg-gray-400'}`} />
          <span className="font-noto text-[10px] text-muted-text">
            {isConnected ? '연결됨' : '연결 대기'}
          </span>
        </div>
      </div>

      {/* 3. Chat Messages Area */}
      <div ref={scrollRef} className="relative z-10 flex-1 overflow-y-auto px-[19.99px] pb-[100px] scrollbar-hide">
        <div className="flex flex-col gap-[17px]">
          {messages.map((msg) => (
            <div key={msg.id} className={`flex items-start gap-[8.5px] ${msg.isOfficial ? 'flex-row-reverse' : ''}`}>
              <UserIcon type={msg.avatarType} variant={msg.avatarVariant} size={23.17} className="flex-shrink-0 mt-[1px]" />
              <div className={`flex flex-col gap-[4.5px] ${msg.isOfficial ? 'items-end' : ''}`}>
                <div className="flex items-center gap-[6px] px-[2px]">
                  <span className={`font-noto text-[12px] leading-[18px] text-[#000000] ${msg.isOfficial ? 'font-bold' : 'font-normal'}`}>
                    {msg.user}
                  </span>
                </div>
                <div className="flex items-end gap-[8px] max-w-[240px]">
                  <div
                    className={`rounded-[15.5px] px-[16px] py-[9.5px] shadow-sm border border-separator/5 ${
                      msg.isOfficial
                        ? 'rounded-tr-none bg-main-bg text-[#000000] font-medium shadow-md shadow-main-bg/10'
                        : 'rounded-tl-none bg-main-bg text-[rgba(0,0,0,0.9)] font-medium shadow-md shadow-main-bg/10'
                    }`}
                  >
                    <p className={`font-noto text-[12px] font-normal tracking-tight ${msg.isOfficial ? 'leading-[12.1px]' : 'leading-[18.2px]'}`}>
                      {msg.text}
                    </p>
                  </div>
                  <span className={`font-noto text-[9px] text-muted-text/50 mb-[2px] flex-shrink-0 ${msg.isOfficial ? 'order-first' : ''}`}>
                    {msg.timestamp}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* 4. Input Area */}
      <div className="absolute bottom-[47px] left-0 right-0 z-30 px-[14px]">
        {isRoomClosed ? (
          <div className="flex h-[39px] items-center justify-center rounded-[21px] bg-sub1 border border-separator">
            <p className="font-noto text-[12px] text-muted-text">채팅방이 종료되었습니다</p>
          </div>
        ) : (
          <>
          {sendError && (
            <p className="mb-[6px] text-center font-noto text-[11px] text-red-400">{sendError}</p>
          )}
          <form onSubmit={handleSend} className="flex items-center gap-[8px]">
            <div className="flex-1 h-[39px]">
              <input
                type="text"
                value={inputText}
                onChange={(e) => setInputText(e.target.value)}
                placeholder="무엇이든 물어보세요!"
                className="w-full h-full rounded-[21px] border border-separator bg-[#FAFAF8] px-[23px] font-noto text-[12px] text-[#000000] placeholder:text-[#B8B8B8] focus:border-point focus:outline-none transition-colors shadow-inner"
              />
            </div>
            <button
              type="submit"
              disabled={!inputText.trim() || isCooldown}
              className="flex h-[39px] w-[73px] items-center justify-center rounded-[21px] bg-point font-noto text-[12px] font-bold text-white shadow-lg shadow-point/20 transition-all hover:bg-[#ff7fa3] active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isCooldown ? '대기중' : '보내기'}
            </button>
          </form>
          </>
        )}
      </div>
    </div>
  )
}
