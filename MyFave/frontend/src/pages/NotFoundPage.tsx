import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-white">
      <div className="text-center">
        <h1 className="mb-4 font-noto text-6xl font-bold text-[#322927]">404</h1>
        <p className="mb-4 font-noto text-lg text-[#8B7E74]">페이지를 찾을 수 없습니다</p>
        <Link to="/" className="font-noto text-sm text-[#FF95B3] hover:underline">
          홈으로 돌아가기
        </Link>
      </div>
    </div>
  )
}
