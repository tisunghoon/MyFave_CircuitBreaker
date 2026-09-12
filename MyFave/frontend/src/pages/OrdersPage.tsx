import { useNavigate } from 'react-router-dom'

import { useConfirmPurchase, useOrdersQuery } from '@/features/orders/hooks'
import type { BackendOrderStatus } from '@/features/orders/types'
import { getProductThumbnail } from '@/features/products/imageMap'
import { SmartImage } from '@/shared/components/SmartImage'

const STATUS_LABEL: Record<BackendOrderStatus, string> = {
  PENDING: '결제 대기',
  PAID: '배송 준비중',
  SHIPPING: '배송 중',
  DELIVERY_COMPLETED: '배송 완료',
  PURCHASE_CONFIRMED: '구매 확정',
  CANCELLED: '주문 취소',
  REFUNDED: '환불 완료',
}

const ACTION_LABEL: Partial<Record<BackendOrderStatus, string>> = {
  SHIPPING: '배송 조회',
  DELIVERY_COMPLETED: '구매 확정',
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })
}

export function OrdersPage() {
  const navigate = useNavigate()
  const confirmPurchase = useConfirmPurchase()
  const { data, isLoading, isError } = useOrdersQuery()
  // PENDING(결제 대기) 주문은 결제가 아직 완료되지 않아 사용자에게 노출하지 않음.
  // (백엔드 list 가 PENDING 을 포함해 내려주더라도 프론트에서 필터링)
  const orders = (data?.content ?? []).filter((o) => o.orderStatus !== 'PENDING')

  if (isLoading) {
    return (
      <div className="flex-1 bg-white p-12 text-center font-noto text-sm text-muted-text">
        불러오는 중...
      </div>
    )
  }

  if (isError) {
    return (
      <div className="flex-1 bg-white p-12 text-center font-noto text-sm text-muted-text">
        주문 내역을 불러오지 못했습니다.
      </div>
    )
  }

  if (orders.length === 0) {
    return (
      <div className="flex-1 bg-white p-12 text-center font-noto text-sm text-muted-text">
        주문 내역이 없습니다.
      </div>
    )
  }

  return (
    <div className="flex-1 bg-white">
      {orders.map((order) => (
        <div key={order.orderId} className="pt-[28.01px]">
          <div className="flex justify-center mb-[34.99px]">
            <span className="font-noto text-[16px] font-medium text-[#000000]">
              {formatDate(order.createdAt)}
            </span>
          </div>

          <div className="flex flex-col gap-[11.99px] px-[19.99px] pb-[22.37px]">
            {order.orderItems.map((item) => (
              <div
                key={item.productId}
                onClick={() => navigate(`/orders/${order.orderId}`)}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault()
                    navigate(`/orders/${order.orderId}`)
                  }
                }}
                className="flex gap-[11.99px] p-[15.99px] rounded-[12px] border-[1.096px] border-[#F2EDEB] bg-white active:bg-gray-50 transition-colors cursor-pointer"
              >
                <div className="w-[84px] h-[84px] flex-shrink-0 overflow-hidden rounded-[15px]">
                  <SmartImage
                    // 로컬 imageMap 우선, 매핑 없으면 백엔드 thumbnailUrl fallback.
                    // HEIC-only 상품도 SmartImage 가 자동 변환 (CR M10).
                    src={getProductThumbnail(item.productId) || item.thumbnailUrl || ''}
                    alt={item.productName}
                    className="w-full h-full object-cover"
                  />
                </div>

                <div className="flex flex-col justify-between flex-1">
                  <div className="flex flex-col gap-[21px]">
                    <div className="flex justify-between items-start">
                      <span className="font-noto text-[11px] font-bold text-[#322927] leading-[15.13px] max-w-[86px]">
                        {item.productName}
                      </span>
                      <span className="font-noto text-[16px] font-bold text-[#CF879B] leading-[24px]">
                        {item.price.toLocaleString()}원
                      </span>
                    </div>

                    <div className="flex items-center justify-between">
                      <span className="font-noto text-[11px] text-[#949494]">
                        {STATUS_LABEL[order.orderStatus]}
                      </span>
                      {ACTION_LABEL[order.orderStatus] && (
                        <button
                          type="button"
                          className="w-[140px] h-[35px] flex items-center justify-center border border-[#F2EDEB] rounded-[5px] font-noto text-[15px] font-medium text-[#949494] hover:bg-gray-100 active:scale-[0.98] transition-all"
                          onClick={(e) => {
                            e.stopPropagation()
                            if (order.orderStatus === 'SHIPPING') {
                              navigate(`/shipping-status/${order.orderId}`)
                            } else if (order.orderStatus === 'DELIVERY_COMPLETED') {
                              confirmPurchase.mutate(order.orderId)
                            }
                          }}
                        >
                          {ACTION_LABEL[order.orderStatus]}
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>

          <div className="px-[30px] pb-[24px]">
            <div className="flex justify-between items-center h-[24px]">
              <span className="font-noto text-[12px] font-normal text-[#000000]">결제 금액</span>
              <span className="font-noto text-[12px] font-bold text-[#CF879B]">
                {order.totalPaymentPrice.toLocaleString()}원
              </span>
            </div>
          </div>

          <div className="h-[8px] w-full bg-[#EFE9E0]" />
        </div>
      ))}
    </div>
  )
}
