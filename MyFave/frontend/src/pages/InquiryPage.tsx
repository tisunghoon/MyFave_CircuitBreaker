import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { PopUp } from '@/shared/components/PopUp'

const INQUIRY_CATEGORIES = ['주문/결제', '배송', '교환/반품', '회원정보', '기타']

export function InquiryPage() {
  const navigate = useNavigate()
  const [category, setCategory] = useState('')
  const [title, setTitle] = useState('')
  const [body, setBody] = useState('')
  const [isCategoryOpen, setIsCategoryOpen] = useState(false)
  const [isPopUpOpen, setIsPopUpOpen] = useState(false)

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!category || !title || !body) return
    setIsPopUpOpen(true)
    setTimeout(() => navigate('/faq'), 2200)
  }

  return (
    <div className="flex-1 bg-white pb-10">
      <div className="border-b border-separator px-5 py-8">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="mb-3 flex items-center gap-1 font-noto text-[13px] font-medium text-[#8B7E74] active:opacity-60 transition-opacity"
          aria-label="뒤로 가기"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="15 18 9 12 15 6" />
          </svg>
          <span>뒤로</span>
        </button>
        <h1 className="font-noto text-xl font-bold text-dark-text tracking-tight">1:1 문의하기</h1>
        <p className="mt-1 font-noto text-xs text-muted-text">담당자가 확인 후 영업일 기준 2일 이내 답변 드립니다.</p>
      </div>

      <form onSubmit={handleSubmit} className="mx-auto max-w-md px-5 pt-6 space-y-5">
        <div className="space-y-2">
          <label className="font-noto text-[13px] font-bold text-dark-text">문의 유형</label>
          <div className="relative">
            <button
              type="button"
              onClick={() => setIsCategoryOpen((v) => !v)}
              className="w-full h-[44px] rounded-[8px] border border-separator bg-white px-4 flex items-center justify-between font-noto text-[13px] text-dark-text active:bg-gray-50 transition-colors"
            >
              <span className={category ? 'text-dark-text' : 'text-[#999999]'}>
                {category || '유형을 선택해주세요'}
              </span>
              <svg width="10" height="6" viewBox="0 0 10 6" fill="none" className={`text-[#949494] transition-transform ${isCategoryOpen ? 'rotate-180' : ''}`}>
                <path d="M1 1L5 5L9 1" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </button>

            {isCategoryOpen && (
              <div className="absolute top-[46px] left-0 right-0 z-30 rounded-[8px] border border-separator bg-white shadow-lg overflow-hidden">
                {INQUIRY_CATEGORIES.map((c) => (
                  <button
                    type="button"
                    key={c}
                    onClick={() => {
                      setCategory(c)
                      setIsCategoryOpen(false)
                    }}
                    className="w-full px-4 py-3 text-left font-noto text-[13px] text-dark-text hover:bg-main-bg transition-colors border-b border-separator/10 last:border-0"
                  >
                    {c}
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="space-y-2">
          <label className="font-noto text-[13px] font-bold text-dark-text">제목</label>
          <input
            type="text"
            placeholder="제목을 입력해주세요"
            className="w-full h-[44px] rounded-[8px] border border-separator bg-white px-4 font-noto text-[13px] text-dark-text placeholder:text-[#999999] focus:border-point focus:outline-none transition-colors"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            maxLength={50}
          />
        </div>

        <div className="space-y-2">
          <label className="font-noto text-[13px] font-bold text-dark-text">내용</label>
          <textarea
            placeholder="문의 내용을 자세히 적어주세요"
            className="w-full min-h-[180px] rounded-[8px] border border-separator bg-white px-4 py-3 font-noto text-[13px] text-dark-text placeholder:text-[#999999] focus:border-point focus:outline-none transition-colors resize-none"
            value={body}
            onChange={(e) => setBody(e.target.value)}
            maxLength={1000}
          />
          <p className="text-right font-noto text-[11px] text-muted-text">{body.length}/1000</p>
        </div>

        <button
          type="submit"
          disabled={!category || !title || !body}
          className="w-full h-[48px] rounded-[12px] bg-point font-noto text-[15px] font-bold text-white shadow-md active:scale-[0.98] transition-all disabled:bg-gray-300"
        >
          문의 접수하기
        </button>
      </form>

      <PopUp
        isOpen={isPopUpOpen}
        message="문의가 접수되었습니다 ✅"
        onClose={() => setIsPopUpOpen(false)}
      />
    </div>
  )
}
