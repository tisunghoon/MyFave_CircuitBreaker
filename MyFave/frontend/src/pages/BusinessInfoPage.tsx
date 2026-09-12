export function BusinessInfoPage() {
  return (
    <div className="flex-1 bg-white pb-10">
      <div className="px-5 py-8">
        <table className="w-full font-noto text-sm">
          <tbody className="divide-y divide-separator/40">
            {[
              { label: '상호명', value: '마이페이브(MyFave)' },
              { label: '대표자', value: '이현영' },
              { label: '사업자등록번호', value: '658-47-01216' },
              { label: '통신판매업신고번호', value: '제 2026-인천부평-0795' },
              { label: '주소', value: '인천광역시 부평구 산청로97' },
              { label: '이메일', value: 'team.myfave@gmail.com' },
              { label: '고객센터', value: '평일 11:00 ~ 18:00' },
            ].map(({ label, value }) => (
              <tr key={label}>
                <td className="w-[110px] py-3.5 font-medium text-muted-text">{label}</td>
                <td className="py-3.5 text-dark-text">{value}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
