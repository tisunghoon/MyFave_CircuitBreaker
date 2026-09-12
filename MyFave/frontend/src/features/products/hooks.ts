import { useQuery } from '@tanstack/react-query'

import { productsApi } from './api'
import { getProductImages, getProductThumbnail } from './imageMap'
import type {
  InfluencerPick,
  Product,
  ProductApiItem,
  ProductCategory,
  ProductDetail,
  ProductDetailApiResponse,
} from './types'

// DAON'S PICK: 픽1~4.mp4 영상 순서와 매칭되는 상품 product_id (고정)
// 1) 원오프 넘버링 티셔츠(2), 2) 브라운 무스탕 자켓(8), 3) 스트라이프 카디건(3), 4) 플라워 롱스커트(10)
const INFLUENCER_PICK_IDS = [2, 8, 3, 10] as const

function mapCategory(code: string | null | undefined): ProductCategory {
  switch (code?.toUpperCase()) {
    case 'BOTTOM': return 'bottom'
    case 'OUTER': return 'outer'
    case 'ACCESSORY': return 'accessory'
    default: return 'top'
  }
}

function toProduct(item: ProductApiItem): Product {
  // 로컬 imageMap(GitHub raw jpg) 우선 — 큐레이션된 이미지 URL 보장.
  // 매핑 없는 신상품은 백엔드 thumbnailUrl 로 fallback (없으면 빈 문자열 → alt 노출).
  const image = getProductThumbnail(item.id) || item.thumbnailUrl || ''
  return {
    id: item.id,
    title: item.productName,
    image,
    price: item.price.toLocaleString() + '원',
    category: mapCategory(item.categoryCode),
    isSoldOut: item.isSoldOut,
  }
}

function toInfluencerPick(item: ProductApiItem): InfluencerPick {
  return { ...toProduct(item), rating: '5.0' }
}

function toProductDetail(item: ProductDetailApiResponse): ProductDetail {
  // 로컬 imageMap 우선 — GitHub 단독샷 jpg 1장 큐레이션.
  // 매핑 없는 신상품은 백엔드 images[] (isMain → sortOrder → 원순서) 정렬해서 fallback.
  const localImages = getProductImages(item.id)
  let images: string[] = localImages
  if (localImages.length === 0 && item.images.length > 0) {
    images = [...item.images]
      .sort((a, b) => {
        if (a.isMain !== b.isMain) return a.isMain ? -1 : 1
        return a.sortOrder - b.sortOrder
      })
      .map((i) => i.imageUrl)
      .filter((url) => !!url)
  }
  return {
    id: item.id,
    title: item.productName,
    subtitle: item.shortReview ?? '',
    price: item.price.toLocaleString() + '원',
    priceNumber: item.price,
    images,
    features: item.description
      ? [{ title: '상품 설명', description: item.description }]
      : [],
  }
}

// useProducts / useInfluencerPicks 가 동일한 상품 리스트를 다른 형태로 가공하므로
// queryKey/queryFn 을 통일하고 select 로 변환만 분리한다 (캐시 1회 공유).
function useProductList<T>(select: (items: ProductApiItem[]) => T) {
  return useQuery({
    queryKey: ['products'],
    queryFn: async () => {
      const data = await productsApi.getList(0, 50)
      return data.content
    },
    select,
  })
}

export function useProducts() {
  return useProductList((items) => items.map(toProduct))
}

export function useInfluencerPicks() {
  return useProductList((items) => {
    const byId = new Map(items.map((item) => [item.id, item]))
    return INFLUENCER_PICK_IDS
      .map((id) => byId.get(id))
      .filter((item): item is ProductApiItem => item != null)
      .map(toInfluencerPick)
  })
}

export function useProduct(id: number | undefined) {
  return useQuery({
    queryKey: ['product', id],
    queryFn: () => productsApi.getDetail(id!).then(toProductDetail),
    enabled: id != null,
  })
}
