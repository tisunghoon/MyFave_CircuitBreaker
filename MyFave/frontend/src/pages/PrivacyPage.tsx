export function PrivacyPage() {
  return (
    <div className="flex-1 bg-white pb-10">
      <div className="space-y-8 px-5 py-8">
        <section>
          <h2 className="mb-3 font-noto text-base font-bold text-dark-text">수집하는 개인정보 항목</h2>
          <ul className="space-y-1.5 font-noto text-sm leading-relaxed text-muted-text">
            <li>• 필수: 이름, 이메일 주소, 비밀번호, 휴대폰 번호</li>
            <li>• 구매 시: 배송지 정보(수령인, 주소, 연락처)</li>
            <li>• 자동 수집: 접속 IP, 쿠키, 서비스 이용 기록</li>
          </ul>
        </section>

        <section>
          <h2 className="mb-3 font-noto text-base font-bold text-dark-text">개인정보의 수집 및 이용 목적</h2>
          <ul className="space-y-1.5 font-noto text-sm leading-relaxed text-muted-text">
            <li>• 회원 가입 및 본인 확인</li>
            <li>• 주문·결제·배송 처리</li>
            <li>• 고객 상담 및 불만 처리</li>
            <li>• 서비스 개선 및 마케팅 활용 (동의 시)</li>
          </ul>
        </section>

        <section>
          <h2 className="mb-3 font-noto text-base font-bold text-dark-text">개인정보의 보유 및 이용 기간</h2>
          <ul className="space-y-1.5 font-noto text-sm leading-relaxed text-muted-text">
            <li>• 회원 탈퇴 시 즉시 파기 (단, 관련 법령에 따라 일정 기간 보관)</li>
            <li>• 전자상거래법에 따른 거래 기록: 5년 보관</li>
          </ul>
        </section>

        <section>
          <h2 className="mb-3 font-noto text-base font-bold text-dark-text">개인정보 보호 책임자</h2>
          <p className="font-noto text-sm leading-relaxed text-muted-text">
            이메일: info@myfave.kr
          </p>
        </section>

        <p className="font-noto text-xs text-muted-text/60">시행일: 2026년 1월 1일</p>
      </div>
    </div>
  )
}
