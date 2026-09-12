import { Link, useSearchParams } from 'react-router-dom'

import { useProducts } from '@/features/products/hooks'
import { SmartImage } from '@/shared/components/SmartImage'

const CATEGORIES = [
  { label: '전체', value: 'all' },
  { label: '상의', value: 'top' },
  { label: '하의', value: 'bottom' },
  { label: '아우터', value: 'outer' },
  { label: '악세사리', value: 'accessory' },
]

export function ProductListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const selectedCategory = searchParams.get('category') ?? 'all'
  // 로딩/에러 상태를 빈 배열로 숨기면 장애가 "상품 0개" 로 묻혀 사용자에게 혼란을 줌 (CR M12).
  const { data: products = [], isLoading, isError } = useProducts()

  const filteredProducts =
    selectedCategory === 'all'
      ? products
      : products.filter((p) => p.category === selectedCategory)

  return (
    <div className="flex-1 bg-white">
      {/* 1. Category tabs - 상단 네비게이션바 바로 아래 고정 (top-0) */}
      <div className="sticky top-0 z-30 h-[44.18px] border-b-[1.096px] border-separator bg-white">
        <div className="mx-auto max-w-[376.04px] px-[19.99px] h-full flex items-center">
          <div className="flex gap-[16px] whitespace-nowrap overflow-x-auto scrollbar-hide h-full items-center">
            {CATEGORIES.map((cat) => (
              <button
                key={cat.value}
                onClick={() =>
                  cat.value === 'all'
                    ? setSearchParams({})
                    : setSearchParams({ category: cat.value })
                }
                className={`h-full font-noto text-[12px] font-medium transition-all ${
                  selectedCategory === cat.value
                    ? 'text-dark-text border-b-[1.096px] border-dark-text'
                    : 'text-muted-text hover:text-dark-text/70'
                }`}
              >
                {cat.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* 2. Product count - 카테고리 탭 바로 아래 위치 (간격 축소) */}
      <div className="mx-auto max-w-[376.04px] bg-white h-[40px] px-[19.99px] flex items-center">
        <p className="font-noto text-[12px] font-normal text-dark-text">
          상품 <span className="font-medium text-black">{filteredProducts.length}</span>개
        </p>
      </div>

      {/* 3. Products grid - 하단 상품 카드 리스트 (각진 모서리 반영) */}
      <div className="mx-auto max-w-[376.04px] px-[21.32px] pt-[12px] pb-[32px] bg-white">
        {isLoading ? (
          <div className="grid grid-cols-2 gap-x-[11.36px] gap-y-[21px]">
            {[0, 1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="h-[200px] bg-gray-100 animate-pulse border border-separator/10" />
            ))}
          </div>
        ) : isError ? (
          <p className="py-16 text-center font-noto text-[13px] text-muted-text">
            상품 목록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.
          </p>
        ) : filteredProducts.length === 0 ? (
          <p className="py-16 text-center font-noto text-[13px] text-muted-text">
            해당 카테고리에 상품이 없습니다.
          </p>
        ) : (
          <div className="grid grid-cols-2 gap-x-[11.36px] gap-y-[21px]">
            {filteredProducts.map((product) => (
              <Link
                key={product.id}
                to={`/product/${product.id}`}
                className="group flex flex-col w-full"
              >
                {/* Product Image - 각진 모서리(rounded-none) */}
                <div className="relative mb-[9px] h-[200px] overflow-hidden bg-gray-50 border border-separator/10">
                  <SmartImage
                    src={product.image}
                    alt={product.title}
                    className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
                  />
                </div>

                {/* Product Info */}
                <div className="px-[1.98px] flex flex-col gap-[3.99px]">
                  <h3 className="min-h-[40px] font-noto text-[12px] font-normal leading-[20px] text-dark-text line-clamp-2 tracking-tight">
                    {product.title}
                  </h3>
                  <p className="font-noto text-[14px] font-bold text-black leading-[18px]">
                    {product.price}
                  </p>
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
