export function ShippingInfoPage() {
  return (
    <div className="flex-1 bg-white pb-10">
      <div className="space-y-8 px-5 py-8">
        <section>
          <h2 className="mb-3 font-noto text-base font-bold text-dark-text">배송비 안내</h2>
          <ul className="space-y-1.5 font-noto text-sm leading-relaxed text-muted-text">
            <li>• 기본 배송비: 3,000원</li>
            <li>• 제주 및 도서산간 지역은 추가 배송비가 발생할 수 있습니다.</li>
          </ul>
        </section>

        <section>
          <h2 className="mb-3 font-noto text-base font-bold text-dark-text">배송 기간</h2>
          <ul className="space-y-1.5 font-noto text-sm leading-relaxed text-muted-text">
            <li>• 주문 확인 후 1~2 영업일 내 출고</li>
            <li>• 출고 후 배송완료까지 약 2~3 영업일 소요</li>
            <li>• 토·일·공휴일은 배송이 진행되지 않습니다.</li>
          </ul>
        </section>

        <section>
          <h2 className="mb-3 font-noto text-base font-bold text-dark-text">주문 취소</h2>
          <ul className="space-y-1.5 font-noto text-sm leading-relaxed text-muted-text">
            <li>• 출고 전: 마이페이지 &gt; 주문 조회에서 직접 취소 가능</li>
            <li>• 출고 후: 고객센터 문의 후 처리 (인스타그램 DM)</li>
          </ul>
        </section>

        <section>
          <h2 className="mb-3 font-noto text-base font-bold text-dark-text">교환 / 반품</h2>
          <ul className="space-y-1.5 font-noto text-sm leading-relaxed text-muted-text">
            <li>• 중고 의류 특성상 교환 및 환불은 불가합니다.</li>
            <li>• 상품 불량·오배송의 경우 수령 후 3일 이내 고객센터로 문의해 주세요.</li>
          </ul>
        </section>
      </div>
    </div>
  )
}
