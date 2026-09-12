interface NoticeItem {
  id: number
  title: string
  content: string
}

const NOTICES: NoticeItem[] = [
  {
    id: 1,
    title: 'MyFave 마켓 오픈 안내',
    content: 'MyFave 마켓은 2026년 5월 25일 00시 오픈 예정입니다.<br>많은 관심 부탁드려요 !♥︎',
  },
  {
    id: 2,
    title: '라이브 채팅 안내',
    content:
      '💬 플리마켓 시작 30분 전, MyFave 홈페이지 메인에서 라이브 채팅을 진행합니다. 무엇이든 물어보세요! 라이브 채팅에 참여하시는 분들 중 추첨을 통해 배송비 무료 쿠폰을 드립니다 ★',
  },
  {
    id: 3,
    title: '1:1 문의',
    content: '1:1 문의는 DM(@daonmoood)으로 부탁드립니다!<br>최대한 빠르게 답변 드릴게요 :)',
  },
]

export function NoticePage() {
  return (
    <div className="flex-1 bg-white pb-10">
      <div className="mx-auto max-w-md divide-y divide-separator/50">
        {NOTICES.map((notice) => (
          <div
            key={notice.id}
            className="group cursor-pointer px-5 py-6 transition-all hover:bg-gray-50 active:bg-gray-100"
          >
            <div className="mb-2 flex items-center gap-2">
              {notice.id === 1 && (
                <span className="rounded bg-main-bg px-1.5 py-0.5 font-noto text-[10px] font-black text-point">NEW</span>
              )}
            </div>
            <h3 className="mb-2 font-noto text-base font-bold text-dark-text leading-snug group-hover:text-point transition-colors">
              {notice.title}
            </h3>
            <p
              className="font-noto text-[13px] leading-relaxed text-dark-text/70"
              dangerouslySetInnerHTML={{ __html: notice.content }}
            />
          </div>
        ))}
      </div>

    </div>
  )
}
