import { Link } from 'react-router-dom'

import { useInfluencerPicks } from '@/features/products/hooks'
import { LiveChatPreview } from '@/shared/components/LiveChatPreview'
import { SmartImage } from '@/shared/components/SmartImage'

const S3 = 'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com'

const MAIN_REEL = `${S3}/%E1%84%8E%E1%85%AC%E1%84%8C%E1%85%A9%E1%86%BC%E1%84%87%E1%85%A9%E1%86%AB.mp4`

const DAON_PICKS = [
  `${S3}/%E1%84%91%E1%85%B5%E1%86%A81.mp4`,
  `${S3}/%E1%84%91%E1%85%B5%E1%86%A82.mp4`,
  `${S3}/%E1%84%91%E1%85%B5%E1%86%A83.mp4`,
  `${S3}/%E1%84%91%E1%85%B5%E1%86%A84.mp4`,
]

export function MainPage() {
  // 로딩/에러 상태를 빈 배열로 숨기면 사용자에게 정상 빈 데이터처럼 오해를 주므로 명시적 분기 (CR M11).
  const { data: influencerProducts = [], isLoading, isError } = useInfluencerPicks()

  return (
    <div className="flex-1 bg-white">
      {/* 1. Main Reels Banner */}
      <div className="mx-auto max-w-md px-[19.99px] pt-[18.01px] pb-[41.28px]">
        <div className="relative overflow-hidden rounded-[16px] bg-white border-0 shadow-figma-card h-[635.72px]">
          {/* Browser-like Header */}
          <div className="flex h-[40px] items-center gap-[8px] bg-main-bg px-[12px] border-0">
            <div className="h-[12px] w-[12px] rounded-full bg-[#FF5F57] shadow-sm" />
            <div className="h-[12px] w-[12px] rounded-full bg-[#FFBD2E] shadow-sm" />
            <div className="h-[12px] w-[12px] rounded-full bg-[#28C840] shadow-sm" />
          </div>
          <video
            src={MAIN_REEL}
            autoPlay
            muted
            loop
            playsInline
            className="w-full h-[595.72px] object-cover border-0"
          />
        </div>
      </div>

      {/* 2. Live Chat Section */}
      <div className="mx-auto max-w-md px-[19.99px] pb-[39.72px]">
        <LiveChatPreview />
      </div>

      {/* 3. Photo Grid Section (2*2) - 라이브 채팅 밑 */}
      <div className="mx-auto max-w-md px-[20px] pb-[40px]">
        {isLoading ? (
          <div className="grid grid-cols-2 gap-[12px]">
            {[0, 1, 2, 3].map((i) => (
              <div key={i} className="aspect-square rounded-[8px] bg-gray-100 animate-pulse" />
            ))}
          </div>
        ) : isError ? (
          <p className="py-10 text-center font-noto text-[13px] text-muted-text">
            상품을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.
          </p>
        ) : (
          <div className="grid grid-cols-2 gap-[12px]">
            {influencerProducts.map((product) => (
              <Link
                key={product.id}
                to={`/product/${product.id}`}
                className="relative aspect-square overflow-hidden rounded-[8px] bg-gray-50 border border-separator/10 active:scale-[0.98] transition-transform"
              >
                <SmartImage
                  src={product.image}
                  alt={product.title}
                  className="h-full w-full object-cover"
                />
              </Link>
            ))}
          </div>
        )}
      </div>

      {/* 4. DAON'S PICK Section (Horizontal Scroll) */}
      <div className="mx-auto max-w-md pt-[23.99px] pb-20">
        <h2 className="px-[19.99px] mb-[15.99px] font-noto text-[16px] font-medium leading-[25.2px] text-dark-text tracking-tight uppercase">DAON'S PICK</h2>

        {isLoading ? (
          <div className="flex gap-[12px] px-[19.99px]">
            {[0, 1, 2].map((i) => (
              <div key={i} className="h-[288.06px] w-[162.03px] flex-shrink-0 rounded-[8px] bg-gray-100 animate-pulse" />
            ))}
          </div>
        ) : isError ? null : (
        <div className="flex overflow-x-auto pb-4 gap-[12px] px-[19.99px] scrollbar-hide">
          {influencerProducts.map((product, index) => (
            <div
              key={product.id}
              className="group flex flex-col flex-shrink-0 w-[162.03px]"
            >
              <div className="relative mb-3 h-[288.06px] overflow-hidden rounded-[8px] bg-white border-0 shadow-sm">
                <video
                  src={DAON_PICKS[index % DAON_PICKS.length]}
                  autoPlay
                  muted
                  loop
                  playsInline
                  className="w-full h-full object-cover border-0"
                />
              </div>
              <Link to={`/product/${product.id}`} className="px-0.5">
                <h3 className="mb-0.5 h-9 font-noto text-[10px] font-normal text-dark-text line-clamp-2 leading-[18px] group-hover:text-point transition-colors tracking-tight whitespace-normal">
                  {product.title}
                </h3>
                <p className="font-noto text-[12px] font-medium text-point leading-[18px]">{product.price}</p>
              </Link>
            </div>
          ))}
        </div>
        )}
      </div>
    </div>
  )
}
