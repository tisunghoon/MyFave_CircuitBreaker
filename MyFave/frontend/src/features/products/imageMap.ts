// 데이터 출처: frontend/상품 정보.md + GitHub asset 레포
// 이미지 호스팅: https://github.com/2026-first-half-of-year-Techeer-Team-D/asset (main)
// 백엔드 응답의 thumbnailUrl/images[].imageUrl을 신뢰하지 않고 프론트엔드에서 직접 매핑한다.
// S3 + HEIC(브라우저 미지원 → heic2any 변환 필요) 구조를 GitHub raw jpg/jpeg 로 교체.
//   → 모든 상품이 즉시 렌더 가능한 jpg 1장(단독샷)을 가지므로 HEIC 변환 경로가 동작하지 않는다.
// 관련 spec: .omc/specs/deep-interview-frontend-image-broken.md

// raw.githubusercontent 베이스 (CDN 캐시됨)
const ASSET_BASE = 'https://raw.githubusercontent.com/2026-first-half-of-year-Techeer-Team-D/asset/main'

// '단독샷' — git 에 저장된 한글 파일명이 NFD(분해형 자모)로 정규화되어 있어 그 형태로 인코딩한다.
// (NFC 로 인코딩하면 raw.githubusercontent 가 404 를 반환함)
const SOLO_SUFFIX = '%E1%84%83%E1%85%A1%E1%86%AB%E1%84%83%E1%85%A9%E1%86%A8%E1%84%89%E1%85%A3%E1%86%BA'

export interface ImageMapEntry {
  slug: string
  /** GitHub asset 단독샷 이미지 URL (jpg 또는 jpeg) */
  url: string
}

// id → { slug, 확장자 }. top3~5 만 .jpeg, 나머지는 .jpg.
// key 는 실제 DB product_id(2~20) 와 일치한다. (product_id=1 은 삭제된 테스트 상품 자리라 비어 있음)
// INFLUENCER_PICK_IDS 도 동일한 product_id 체계를 따른다.
const ASSET_TABLE: Partial<Record<number, { slug: string; ext: 'jpg' | 'jpeg' }>> = {
  2: { slug: 'top1', ext: 'jpg' },
  3: { slug: 'top2', ext: 'jpg' },
  4: { slug: 'top3', ext: 'jpeg' },
  5: { slug: 'top4', ext: 'jpeg' },
  6: { slug: 'top5', ext: 'jpeg' },
  7: { slug: 'top6', ext: 'jpg' },
  8: { slug: 'outer1', ext: 'jpg' },
  9: { slug: 'outer2', ext: 'jpg' },
  10: { slug: 'bottom1', ext: 'jpg' },
  11: { slug: 'bottom2', ext: 'jpg' },
  12: { slug: 'top7', ext: 'jpg' },
  13: { slug: 'top8', ext: 'jpg' },
  14: { slug: 'top9', ext: 'jpg' },
  15: { slug: 'top10', ext: 'jpg' },
  16: { slug: 'top11', ext: 'jpg' },
  17: { slug: 'bottom3', ext: 'jpg' },
  18: { slug: 'bottom4', ext: 'jpg' },
  19: { slug: 'acc1', ext: 'jpg' },
  20: { slug: 'acc2', ext: 'jpg' },
}

export const imageMap: Partial<Record<number, ImageMapEntry>> = Object.fromEntries(
  Object.entries(ASSET_TABLE).map(([id, { slug, ext }]) => [
    Number(id),
    { slug, url: `${ASSET_BASE}/${slug}_${SOLO_SUFFIX}.${ext}` },
  ]),
)

export function getProductThumbnail(id: number): string {
  return imageMap[id]?.url ?? ''
}

export function getProductImages(id: number): string[] {
  const url = imageMap[id]?.url
  return url ? [url] : []
}
