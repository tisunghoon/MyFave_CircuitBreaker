import axios from 'axios'
import { format } from 'date-fns'
import { useParams } from 'react-router-dom'

import { useTracking } from '@/features/shipping/hooks'

// Tracker.delivery carrierId → 사용자 표시용 한글 택배사명.
// 새로운 캐리어가 추가되면 여기 한 줄 더 매핑.
const CARRIER_NAMES: Record<string, string> = {
  'kr.cjlogistics': 'CJ대한통운',
  'kr.epost': '우체국택배',
  'kr.hanjin': '한진택배',
  'kr.logen': '로젠택배',
  'kr.lotte': '롯데택배',
  'kr.kdexp': '경동택배',
  'kr.cupost': 'CU편의점택배',
  'kr.daesin': '대신택배',
  'kr.gspostbox': 'GSPostbox',
}

const STEPS = ['결제완료', '배송준비중', '배송중', '배송완료'] as const

// Tracker.delivery statusCode → 4-step 진행도 index.
// 알 수 없는 코드는 1(배송준비중)로 안전 fallback.
function getStepIndex(statusCode: string | undefined | null): number {
  if (!statusCode) return 1
  if (statusCode === 'DELIVERED') return 3
  if (['OUT_FOR_DELIVERY', 'SHIPPING', 'AT_HUB', 'IN_TRANSIT'].includes(statusCode)) return 2
  return 1
}

function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const errorCode = error.response?.data?.errorCode
    if (errorCode === 'TRACKING_NOT_REGISTERED') return '운송장이 아직 등록되지 않았습니다.'
    if (errorCode === 'TRACKING_API_ERROR') return '배송 추적 서버 오류. 잠시 후 다시 시도해주세요.'
  }
  return '배송 정보를 불러올 수 없습니다.'
}

export function ShippingStatusPage() {
  const { orderId } = useParams()
  const { data, isLoading, error } = useTracking(orderId ? Number(orderId) : undefined)

  const currentStep = getStepIndex(data?.statusCode)
  const carrierLabel = data?.carrierId ? CARRIER_NAMES[data.carrierId] ?? data.carrierId : undefined

  return (
    <div className="flex-1 bg-white pb-20">
      <div className="px-[19.99px] py-[24px]">
        {/* ─ 배송 현황 ─────────────────────────────────────────────── */}
        <h1 className="font-noto text-[18px] font-bold text-dark-text mb-[16px]">배송 현황</h1>

        {/* 4-step 진행도 — 이미지의 "결제완료 ▸ 배송준비중 ▸ 배송중 ▸ 배송완료" 가로 배치.
            현재 단계는 point 컬러로 강조. */}
        <div className="mb-[28px] rounded-[8px] border-[1.096px] border-[#F2EDEB] bg-footer-bg px-[12px] py-[14px]">
          <div className="flex items-center justify-between">
            {STEPS.map((step, idx) => (
              <div key={step} className="flex items-center">
                <span
                  className={`font-noto text-[12px] tracking-tight ${
                    idx === currentStep
                      ? 'font-bold text-point'
                      : idx < currentStep
                        ? 'font-medium text-dark-text'
                        : 'font-normal text-muted-text'
                  }`}
                >
                  {step}
                </span>
                {idx < STEPS.length - 1 && (
                  <span className="px-[6px] text-[10px] text-muted-text" aria-hidden>
                    ›
                  </span>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* 로딩 스켈레톤 */}
        {isLoading && (
          <div className="space-y-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-12 rounded-lg bg-gray-100 animate-pulse" />
            ))}
          </div>
        )}

        {/* 에러 */}
        {error && !isLoading && (
          <div className="py-16 text-center">
            <p className="font-noto text-[14px] text-muted-text">{getErrorMessage(error)}</p>
          </div>
        )}

        {/* 데이터 정상 — 배송 이력 + 배송 정보 */}
        {data && !error && (
          <>
            {/* 배송 이력 테이블 — 처리 일시 / 현재 위치 / 상태 (이미지 그대로) */}
            <div className="overflow-hidden">
              <table className="w-full border-collapse">
                <thead>
                  <tr className="border-b border-[#F2EDEB]">
                    <th className="py-[10px] text-left font-noto text-[12px] font-bold text-dark-text">
                      처리 일시
                    </th>
                    <th className="py-[10px] text-left font-noto text-[12px] font-bold text-dark-text">
                      현재 위치
                    </th>
                    <th className="py-[10px] text-left font-noto text-[12px] font-bold text-dark-text">
                      상태
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {data.events.length === 0 ? (
                    <tr>
                      <td
                        colSpan={3}
                        className="py-[32px] text-center font-noto text-[12px] text-muted-text"
                      >
                        배송 이력이 없습니다.
                      </td>
                    </tr>
                  ) : (
                    data.events.map((event, index) => (
                      <tr
                        key={`${event.time}-${index}`}
                        className="border-b border-[#F2EDEB]/60 last:border-0"
                      >
                        <td className="py-[12px] pr-[8px] align-top font-noto text-[11px] text-dark-text leading-[16px] whitespace-pre-line">
                          {format(new Date(event.time), 'yyyy-MM-dd\nHH:mm:ss')}
                        </td>
                        <td className="py-[12px] pr-[8px] align-top font-noto text-[12px] text-dark-text leading-[18px]">
                          {event.location ?? '-'}
                        </td>
                        <td
                          className={`py-[12px] align-top font-noto text-[12px] leading-[18px] ${
                            index === 0 ? 'font-bold text-point' : 'font-medium text-dark-text'
                          }`}
                        >
                          {event.statusName ?? '-'}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            {/* ─ 배송 정보 ───────────────────────────────────────────── */}
            <div className="mt-[36px]">
              <h2 className="font-noto text-[18px] font-bold text-dark-text mb-[16px]">배송 정보</h2>
              <div className="space-y-[12px]">
                <div className="flex">
                  <span className="w-[80px] font-noto text-[12px] text-muted-text">택배사</span>
                  <span className="font-noto text-[12px] text-dark-text">
                    {carrierLabel ?? '-'}
                  </span>
                </div>
                <div className="flex">
                  <span className="w-[80px] font-noto text-[12px] text-muted-text">송장번호</span>
                  <span className="font-noto text-[12px] text-dark-text">
                    {data.trackingNumber ?? '-'}
                  </span>
                </div>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
