export function TermsPage() {
  return (
    <div className="flex-1 bg-white pb-10">
      <div className="space-y-8 px-5 py-8">
        <section>
          <h2 className="mb-3 font-noto text-base font-bold text-dark-text">제1조 (목적)</h2>
          <p className="font-noto text-sm leading-relaxed text-muted-text">
            이 약관은 MY FAVE(이하 "회사")가 제공하는 서비스의 이용 조건 및 절차, 회사와 회원 간의 권리·의무 및 책임 사항을 규정함을 목적으로 합니다.
          </p>
        </section>

        <section>
          <h2 className="mb-3 font-noto text-base font-bold text-dark-text">제2조 (서비스 이용)</h2>
          <ul className="space-y-1.5 font-noto text-sm leading-relaxed text-muted-text">
            <li>• 서비스는 회원가입 후 이용 가능합니다.</li>
            <li>• 회원은 타인의 정보를 도용하거나 허위 정보를 등록할 수 없습니다.</li>
            <li>• 회사는 서비스 운영상 필요한 경우 서비스 내용을 변경할 수 있습니다.</li>
          </ul>
        </section>

        <section>
          <h2 className="mb-3 font-noto text-base font-bold text-dark-text">제3조 (구매 및 결제)</h2>
          <ul className="space-y-1.5 font-noto text-sm leading-relaxed text-muted-text">
            <li>• 상품 구매는 회원만 가능합니다.</li>
            <li>• 결제는 카드, 카카오페이, 네이버페이, 토스페이 등을 지원합니다.</li>
            <li>• 중고 의류 특성상 교환 및 환불은 원칙적으로 불가합니다.</li>
          </ul>
        </section>

        <section>
          <h2 className="mb-3 font-noto text-base font-bold text-dark-text">제4조 (회원 탈퇴)</h2>
          <p className="font-noto text-sm leading-relaxed text-muted-text">
            회원은 언제든지 마이페이지에서 탈퇴를 신청할 수 있으며, 탈퇴 시 관련 법령에 따라 일부 정보는 보관될 수 있습니다.
          </p>
        </section>

        <p className="font-noto text-xs text-muted-text/60">시행일: 2026년 1월 1일</p>
      </div>
    </div>
  )
}
