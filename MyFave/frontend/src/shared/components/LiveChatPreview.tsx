import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { UserIcon } from '@/shared/components/UserIcon'
import { useChatPreview } from '@/features/chat/hooks'
import { getChatLifecycleState } from '@/shared/utils/saleSchedule'

function getVariantFromNickname(nickname: string): number {
  const BEAR_VARIANTS = [1, 3, 5, 6, 7, 10] as const
  let hash = 0
  for (let i = 0; i < nickname.length; i++) {
    hash = (hash + nickname.charCodeAt(i)) % BEAR_VARIANTS.length
  }
  return BEAR_VARIANTS[hash]
}

export function LiveChatPreview() {
  const { data, isLoading, isError } = useChatPreview(5)
  // 라이프사이클(시간 기반) 게이트 — 분 단위 갱신으로 충분.
  const [now, setNow] = useState(() => new Date())
  useEffect(() => {
    const tick = setInterval(() => setNow(new Date()), 60_000)
    return () => clearInterval(tick)
  }, [])
  const lifecycle = getChatLifecycleState({ now })

  if (isLoading) {
    return (
      <div className="rounded-[5px] border border-main-bg bg-main-bg p-[20px] shadow-sm">
        <div className="mb-[16px] flex items-center justify-between px-[2px]">
          <h3 className="font-noto text-[14px] font-bold text-dark-text tracking-tight">라이브 톡</h3>
          <div className="h-4 w-12 rounded bg-gray-200 animate-pulse" />
        </div>
        <div className="space-y-[16px]">
          {[1, 2, 3].map((i) => (
            <div key={i} className="flex items-start gap-[8px]">
              <div className="h-6 w-6 rounded-full bg-gray-200 animate-pulse flex-shrink-0" />
              <div className="flex flex-col gap-2">
                <div className="h-3 w-16 rounded bg-gray-200 animate-pulse" />
                <div className="h-8 w-40 rounded-[15.5px] bg-gray-200 animate-pulse" />
              </div>
            </div>
          ))}
        </div>
        <div className="mt-[20px] h-[46px] w-full rounded-[5px] bg-gray-200 animate-pulse" />
      </div>
    )
  }

  // 진짜 OPEN 은 (시간 기반 OPEN) AND (백엔드 isActive=true) 일 때만.
  const isBackendActive = !isError && data?.isActive === true
  const isOpen = lifecycle === 'OPEN' && isBackendActive
  const isClosed = lifecycle === 'CLOSED'
  const messages = data?.recentMessages ?? []

  return (
    <div className="rounded-[5px] border border-main-bg bg-main-bg p-[20px] shadow-sm">
      <div className="mb-[16px] flex items-center justify-between px-[2px]">
        <h3 className="font-noto text-[14px] font-bold text-dark-text tracking-tight">라이브 톡</h3>
        {isOpen ? (
          <span className="flex items-center gap-1 font-noto text-[10px] text-point">
            <span className="h-1.5 w-1.5 rounded-full bg-point animate-pulse" />
            실시간
          </span>
        ) : isClosed ? (
          <span className="flex items-center gap-1 font-noto text-[10px] text-muted-text">
            <span className="h-1.5 w-1.5 rounded-full bg-gray-400" />
            종료
          </span>
        ) : (
          <span className="flex items-center gap-1 font-noto text-[10px] text-muted-text">
            <span className="h-1.5 w-1.5 rounded-full bg-gray-400" />
            준비중
          </span>
        )}
      </div>

      {isOpen && messages.length > 0 ? (
        <div className="space-y-[16px]">
          {messages.map((msg, i) => (
            <div key={i} className="flex items-start gap-[8px]">
              <UserIcon
                type="bear"
                variant={getVariantFromNickname(msg.senderNickname)}
                size={23.17}
                className="flex-shrink-0"
              />
              <div className="flex flex-col gap-[4px]">
                <span className="font-noto text-[12px] font-normal leading-[18px] text-dark-text/70">
                  {msg.senderNickname}
                </span>
                <div className="max-w-[210px] rounded-[15.5px] rounded-tl-none bg-chat-bg2 px-[16px] py-[10px] shadow-sm">
                  <p className="font-noto text-[12px] leading-[18.2px] font-normal tracking-tight text-chat-font2">
                    {msg.content}
                  </p>
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="flex items-center justify-center py-6">
          <p className="font-noto text-[12px] text-muted-text">
            {isClosed
              ? '라이브 톡이 종료되었습니다'
              : isOpen
                ? '아직 메시지가 없습니다'
                : '오픈 30분 전에 채팅방이 열려요'}
          </p>
        </div>
      )}

      {isOpen ? (
        <Link
          to="/live-chat"
          className="mt-[20px] flex h-[46px] w-full items-center justify-center rounded-[5px] bg-point font-montserrat text-[14px] font-semibold text-chat-bg shadow-lg shadow-point/20 transition-all hover:bg-[#ff7fa3] active:scale-[0.98]"
        >
          라이브 채팅 시작하기
        </Link>
      ) : (
        <button
          type="button"
          disabled
          className="mt-[20px] flex h-[46px] w-full cursor-not-allowed items-center justify-center rounded-[5px] bg-[#E5DCD8] font-montserrat text-[14px] font-semibold text-[#8B7E74] opacity-80"
        >
          {isClosed ? '종료된 라이브 톡' : '잠금 — 오픈 30분 전 자동 해제'}
        </button>
      )}
    </div>
  )
}
