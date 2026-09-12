-- 상품 시드 스크립트 (멱등 — 여러 번 실행해도 동일 결과)
-- 출처: frontend/상품 정보.md (19개 상품)
-- 전제: 인플루언서 user_id = 1 (loadtest1@myfave.test)
-- 정책:
--   - product_id 1~10: order_items FK 보호를 위해 UPDATE (DELETE 금지)
--   - product_id 11~19: UPSERT (INSERT ... ON CONFLICT DO UPDATE)
--   - product_images: 1~19 DELETE 후 재INSERT
--   - 이미지 URL은 마크다운 그대로 사용 (NFD 인코딩, .jpg 대표 + .heic 단독샷)
--   - 마크다운에서 대표사진(.jpg)이 비어있던 11~14번도 실제 S3에는 존재 → jpg 시드
--   - 15~19번(top11/bottom3/bottom4/acc1/acc2)은 S3에도 .jpg 없음 → .heic 1장만 시드
-- 실행:
--   docker exec -i myfave-postgres psql -U postgres -d myfave < backend/scripts/db/seed_products.sql

BEGIN;

-- =====================================================
-- 1) products 1~10: UPDATE (기존 주문 보호)
-- =====================================================

UPDATE products SET
  user_id        = 1,
  product_name   = '원오프 넘버링 티셔츠',
  short_review   = '미착용 (only 피팅)',
  price          = 10000,
  description    = '한쪽 어깨가 시원하게 드러나는 오프숄더 디자인의 화이트 긴팔 티셔츠예요. 빈티지한 느낌의 넘버링 프린트가 포인트로 들어가 캐주얼하면서도 트렌디한 무드를 연출해줍니다. 한 장만 입어도 데일리룩으로 완성도 있게 떨어져요!',
  size           = NULL,
  condition_code = 'S_GRADE',
  category_code  = 'TOP',
  is_soldout     = false,
  stock_quantity = 1,
  updated_at     = NOW(),
  deleted_at     = NULL
WHERE product_id = 1;

UPDATE products SET
  user_id        = 1,
  product_name   = '스트라이프 카디건',
  short_review   = '미착용 (only 피팅)',
  price          = 10000,
  description    = '부드러운 촉감의 그레이 톤 스트라이프 카디건이에요. 두 가지 톤의 그레이가 차분하면서도 세련된 무드를 연출해줍니다. 데일리하게 걸치기 좋고 어떤 하의에도 무난하게 매치되더라구요.',
  size           = NULL,
  condition_code = 'S_GRADE',
  category_code  = 'TOP',
  is_soldout     = false,
  stock_quantity = 1,
  updated_at     = NOW(),
  deleted_at     = NULL
WHERE product_id = 2;

UPDATE products SET
  user_id        = 1,
  product_name   = '오프숄더 리본 니트',
  short_review   = '착용 1번',
  price          = 10000,
  description    = '과하지 않은 레드 컬러에 한쪽 어깨를 리본으로 묶는 디자인이 사랑스러운 니트예요. 허리라인 포인트로 라인을 살려줍니다. 포인트 컬러로 코디에 생기를 더해주는 아이템!',
  size           = NULL,
  condition_code = 'A_GRADE',
  category_code  = 'TOP',
  is_soldout     = false,
  stock_quantity = 1,
  updated_at     = NOW(),
  deleted_at     = NULL
WHERE product_id = 3;

UPDATE products SET
  user_id        = 1,
  product_name   = '스카이 골지 카디건',
  short_review   = '착용 1번',
  price          = 10000,
  description    = '은은한 스카이 컬러가 봄·여름 분위기에 딱 맞는 슬림핏 골지 카디건이에요. 세로 골지 패턴이 슬림한 라인을 더욱 돋보이게 해줍니다. 단독으로 입거나 이너로 활용해도 좋은 활용도 높은 아이템이에요.',
  size           = NULL,
  condition_code = 'A_GRADE',
  category_code  = 'TOP',
  is_soldout     = false,
  stock_quantity = 1,
  updated_at     = NOW(),
  deleted_at     = NULL
WHERE product_id = 4;

UPDATE products SET
  user_id        = 1,
  product_name   = '살구 핑크 레이어드 셔츠',
  short_review   = '미착용 (only 피팅)',
  price          = 10000,
  description    = '은은한 살구빛 핑크 컬러의 레이어드 셔츠예요. 레이어드 디테일 덕에 단조롭지 않고 센스있는 느낌을 줍니다. 도톰하지 않은 가벼운 소재라 봄~초여름에 데일리로 입기 좋아요.',
  size           = NULL,
  condition_code = 'S_GRADE',
  category_code  = 'TOP',
  is_soldout     = false,
  stock_quantity = 1,
  updated_at     = NOW(),
  deleted_at     = NULL
WHERE product_id = 5;

UPDATE products SET
  user_id        = 1,
  product_name   = '배색 스트라이프 니트 카디건',
  short_review   = '미착용 (only 피팅)',
  price          = 10000,
  description    = '클래식한 마린룩 무드의 아이보리 베이스 블랙 스트라이프 카디건이에요. 탄탄한 짜임감으로 봄·가을·겨울에 따뜻하게 입기 좋고, 살짝 오버핏이라 편안하게 떨어집니다. 어떤 스타일에도 잘 녹아드는 베이직 아이템으로 추천해요!',
  size           = NULL,
  condition_code = 'S_GRADE',
  category_code  = 'TOP',
  is_soldout     = false,
  stock_quantity = 1,
  updated_at     = NOW(),
  deleted_at     = NULL
WHERE product_id = 6;

UPDATE products SET
  user_id        = 1,
  product_name   = '브라운 무스탕 자켓',
  short_review   = '착용 1번',
  price          = 30000,
  description    = '포근한 안감과 브라운 외피가 조화로운 무스탕 자켓이에요. 카라와 소매 끝, 밑단까지 풍성한 무스탕이 둘러져 있어 보온성이 뛰어납니다. 청바지와 매치하면 빈티지한 무드의 겨울 코디 완성!',
  size           = NULL,
  condition_code = 'A_GRADE',
  category_code  = 'OUTER',
  is_soldout     = false,
  stock_quantity = 1,
  updated_at     = NOW(),
  deleted_at     = NULL
WHERE product_id = 7;

UPDATE products SET
  user_id        = 1,
  product_name   = '블랙 퍼 카라 레더 자켓',
  short_review   = '미착용 (only 피팅)',
  price          = 30000,
  description    = '탈부착 가능한 화이트 퍼 카라가 포인트인 블랙 레더 자켓이에요. 휘뚤마뚤 데일리하게 입기 좋아 더욱 매력적이구요! 간절기부터 초겨울까지 활용도 높게 입을 수 있어요.',
  size           = NULL,
  condition_code = 'S_GRADE',
  category_code  = 'OUTER',
  is_soldout     = false,
  stock_quantity = 1,
  updated_at     = NOW(),
  deleted_at     = NULL
WHERE product_id = 8;

UPDATE products SET
  user_id        = 1,
  product_name   = '플라워 롱스커트',
  short_review   = '미착용 (only 피팅)',
  price          = 10000,
  description    = '은은하게 비치는 시스루 소재에 플라워 자카드 패턴이 입체적으로 들어간 롱스커트예요. 풍성한 실루엣 덕분에 한 벌만 입어도 우아한 분위기가 연출됩니다. 특히 니트와 잘 어울리더라구요!',
  size           = NULL,
  condition_code = 'S_GRADE',
  category_code  = 'BOTTOM',
  is_soldout     = false,
  stock_quantity = 1,
  updated_at     = NOW(),
  deleted_at     = NULL
WHERE product_id = 9;

UPDATE products SET
  user_id        = 1,
  product_name   = '셔링 롱스커트',
  short_review   = '미착용 (only 피팅)',
  price          = 10000,
  description    = '은은한 광택감이 도는 블랙 컬러의 셔링 디테일 롱스커트예요. 자연스러운 주름이 풍성하게 떨어져 페미닌한 실루엣을 연출해줍니다. 발목까지 떨어지는 기장감으로 키와 상관없이 분위기 있게 소화할 수 있어요.',
  size           = NULL,
  condition_code = 'S_GRADE',
  category_code  = 'BOTTOM',
  is_soldout     = false,
  stock_quantity = 1,
  updated_at     = NOW(),
  deleted_at     = NULL
WHERE product_id = 10;

-- =====================================================
-- 2) product_images 1~10: 기존 삭제 후 재시드 (대표 jpg + 단독 heic)
-- =====================================================

DELETE FROM product_images WHERE product_id BETWEEN 1 AND 10;

INSERT INTO product_images (product_id, image_url, sort_order, is_main, created_at, updated_at) VALUES
(1,  'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/top1_%E1%84%8E%E1%85%A1%E1%86%A8%E1%84%8B%E1%85%AD%E1%86%BC%E1%84%89%E1%85%A3%E1%86%BA.jpg',    1, true,  NOW(), NOW()),
(1,  'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/top1_%E1%84%83%E1%85%A1%E1%86%AB%E1%84%83%E1%85%A9%E1%86%A8%E1%84%89%E1%85%A3%E1%86%BA.heic',   2, false, NOW(), NOW()),
(2,  'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/top2_%E1%84%8E%E1%85%A1%E1%86%A8%E1%84%8B%E1%85%AD%E1%86%BC%E1%84%89%E1%85%A3%E1%86%BA.jpg',    1, true,  NOW(), NOW()),
(2,  'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/top2_%E1%84%83%E1%85%A1%E1%86%AB%E1%84%83%E1%85%A9%E1%86%A8%E1%84%89%E1%85%A3%E1%86%BA.heic',   2, false, NOW(), NOW()),
(3,  'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/top3_%E1%84%8E%E1%85%A1%E1%86%A8%E1%84%8B%E1%85%AD%E1%86%BC%E1%84%89%E1%85%A3%E1%86%BA.jpg',    1, true,  NOW(), NOW()),
(3,  'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/top3_%E1%84%83%E1%85%A1%E1%86%AB%E1%84%83%E1%85%A9%E1%86%A8%E1%84%89%E1%85%A3%E1%86%BA.heic',   2, false, NOW(), NOW()),
(4,  'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/top4_%E1%84%8E%E1%85%A1%E1%86%A8%E1%84%8B%E1%85%AD%E1%86%BC%E1%84%89%E1%85%A3%E1%86%BA.jpg',    1, true,  NOW(), NOW()),
(4,  'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/top4_%E1%84%83%E1%85%A1%E1%86%AB%E1%84%83%E1%85%A9%E1%86%A8%E1%84%89%E1%85%A3%E1%86%BA.heic',   2, false, NOW(), NOW()),
(5,  'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/top5_%E1%84%8E%E1%85%A1%E1%86%A8%E1%84%8B%E1%85%AD%E1%86%BC%E1%84%89%E1%85%A3%E1%86%BA.jpg',    1, true,  NOW(), NOW()),
(5,  'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/top5_%E1%84%83%E1%85%A1%E1%86%AB%E1%84%83%E1%85%A9%E1%86%A8%E1%84%89%E1%85%A3%E1%86%BA.heic',   2, false, NOW(), NOW()),
(6,  'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/top6_%E1%84%8E%E1%85%A1%E1%86%A8%E1%84%8B%E1%85%AD%E1%86%BC%E1%84%89%E1%85%A3%E1%86%BA.jpg',    1, true,  NOW(), NOW()),
(6,  'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/top6_%E1%84%83%E1%85%A1%E1%86%AB%E1%84%83%E1%85%A9%E1%86%A8%E1%84%89%E1%85%A3%E1%86%BA.heic',   2, false, NOW(), NOW()),
(7,  'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/outer1_%E1%84%8E%E1%85%A1%E1%86%A8%E1%84%8B%E1%85%AD%E1%86%BC%E1%84%89%E1%85%A3%E1%86%BA.jpg',  1, true,  NOW(), NOW()),
(7,  'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/outer1_%E1%84%83%E1%85%A1%E1%86%AB%E1%84%83%E1%85%A9%E1%86%A8%E1%84%89%E1%85%A3%E1%86%BA.heic', 2, false, NOW(), NOW()),
(8,  'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/outer2_%E1%84%8E%E1%85%A1%E1%86%A8%E1%84%8B%E1%85%AD%E1%86%BC%E1%84%89%E1%85%A3%E1%86%BA.jpg',  1, true,  NOW(), NOW()),
(8,  'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/outer2_%E1%84%83%E1%85%A1%E1%86%AB%E1%84%83%E1%85%A9%E1%86%A8%E1%84%89%E1%85%A3%E1%86%BA.heic', 2, false, NOW(), NOW()),
(9,  'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/bottom1_%E1%84%8E%E1%85%A1%E1%86%A8%E1%84%8B%E1%85%AD%E1%86%BC%E1%84%89%E1%85%A3%E1%86%BA.jpg', 1, true,  NOW(), NOW()),
(9,  'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/bottom1_%E1%84%83%E1%85%A1%E1%86%AB%E1%84%83%E1%85%A9%E1%86%A8%E1%84%89%E1%85%A3%E1%86%BA.heic',2, false, NOW(), NOW()),
(10, 'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/bottom2_%E1%84%8E%E1%85%A1%E1%86%A8%E1%84%8B%E1%85%AD%E1%86%BC%E1%84%89%E1%85%A3%E1%86%BA.jpg', 1, true,  NOW(), NOW()),
(10, 'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/bottom2_%E1%84%83%E1%85%A1%E1%86%AB%E1%84%83%E1%85%A9%E1%86%A8%E1%84%89%E1%85%A3%E1%86%BA.heic',2, false, NOW(), NOW());

-- =====================================================
-- 3) products 11~19: UPSERT (재실행 안전)
-- =====================================================

INSERT INTO products (product_id, user_id, product_name, short_review, price, description, size, condition_code, category_code, is_soldout, stock_quantity, created_at, updated_at, deleted_at) VALUES
(11, 1, '하트 프릴 맨투맨',          '미착용 (only 피팅)', 20000, '큼직한 하트에 프릴 디테일과 빈티지한 프린팅이 들어간 맨투맨이에요. 러블리한 디자인 덕분에 한 장만 입어도 코디 완성! 미니스커트나 청바지 어디에 매치해도 잘 어울리더라구요',                              NULL, 'S_GRADE', 'TOP',       false, 1, NOW(), NOW(), NULL),
(12, 1, '도트 배색 반팔 티셔츠',     '미착용 (only 피팅)', 10000, '아이보리빛 화이트 바탕에 자잘한 블랙 도트 패턴이 들어갓 슬림핏 반팔 티셔츠! 넥라인에 블랙 배색이 들어가 깔끔하면서도 포인트 있는 디자인이에요. 블랙 컬러 하의에 매치했을 때 특히 예쁘더라구요.',     NULL, 'S_GRADE', 'TOP',       false, 1, NOW(), NOW(), NULL),
(13, 1, '원오프 리본 골지 티셔츠',   '미착용 (only 피팅)', 20000, '라이트그레이 컬러에 한쪽 어깨를 리본으로 묶는 디자인이 사랑스러운 원오프 티셔츠! 슬림한 핏에 골지 짜임에 진짜 팔뚝살 삭제핏! 작은 자수 포인트까지 더해져서 퀄리티가 정말 좋은 아이템이에요.',     NULL, 'S_GRADE', 'TOP',       false, 1, NOW(), NOW(), NULL),
(14, 1, '리본 자수 브이넥 가디건',   '미착용 (only 피팅)', 10000, '리본 자수가 포인트로 들어간 블랙 컬러의 브이넥 가디건이에요. 골드 버튼이 클래식한 무드를 더해줍니다. 단독으로도, 다양한 이너에 레이어드하기에도 좋아서 활용도가 높아요!',                              NULL, 'S_GRADE', 'TOP',       false, 1, NOW(), NOW(), NULL),
(15, 1, '레이어드 끈나시 니트',      '착용 1번',           10000, '은은한 소라 컬러의 끈나시 니트탑이에요. 작은 리본 디테일과 넥라인이 사랑스러운 무드를 더해줍니다. 흰티나 블라우스에 레이어드해주기 딱이에요!',                                                            NULL, 'A_GRADE', 'TOP',       false, 1, NOW(), NOW(), NULL),
(16, 1, '네이비 코튼 숏팬츠',        '미착용 (only 피팅)', 20000, '딥한 네이비 컬러의 깔끔한 베이직 숏팬츠! 군더더기 없는 디자인과 적당한 기장감으로 어떤 상의와도 매치하기 좋더라구요. 여름철 데일리룩으로 활용도가 매우 높은 기본템!',                                 NULL, 'S_GRADE', 'BOTTOM',    false, 1, NOW(), NOW(), NULL),
(17, 1, '베이비 핑크 코튼 숏팬츠',   '미착용 (only 피팅)', 20000, '사랑스러운 베이비 핑크 컬러의 코튼 숏팬츠! 깔끔한 라인의 디자인이라 러블리하면서도 단정한 느낌을 줍니다. 포인트 컬러 하의로 소장가치 높은 아이템!',                                                   NULL, 'S_GRADE', 'BOTTOM',    false, 1, NOW(), NOW(), NULL),
(18, 1, '핑크배색 로고 볼캡',        '미착용 (only 피팅)',  5000, '핑크와 버건디 배색이 포인트되는 볼캡이에요. 빈티지한 와펜이 포인트가 되어 캐주얼한 무드를 살려줍니다.',                                                                                                NULL, 'S_GRADE', 'ACCESSORY', false, 1, NOW(), NOW(), NULL),
(19, 1, '크림베이지 로고 볼캡',      '미착용 (only 피팅)',  5000, '은은한 베이지톤의 볼캡이에요. 같은 톤의 와펜이 들어가 데일리로 활용하기 좋은 아이템입니다!',                                                                                                          NULL, 'S_GRADE', 'ACCESSORY', false, 1, NOW(), NOW(), NULL)
ON CONFLICT (product_id) DO UPDATE SET
  user_id        = EXCLUDED.user_id,
  product_name   = EXCLUDED.product_name,
  short_review   = EXCLUDED.short_review,
  price          = EXCLUDED.price,
  description    = EXCLUDED.description,
  size           = EXCLUDED.size,
  condition_code = EXCLUDED.condition_code,
  category_code  = EXCLUDED.category_code,
  is_soldout     = EXCLUDED.is_soldout,
  stock_quantity = EXCLUDED.stock_quantity,
  updated_at     = NOW(),
  deleted_at     = NULL;

-- =====================================================
-- 4) product_images 11~19: 삭제 후 재시드
--    - 11~14: 대표 .jpg + 단독 .heic (S3에 .jpg 존재 확인됨)
--    - 15~19: .heic 1장만 (S3에 .jpg 없음)
-- =====================================================

DELETE FROM product_images WHERE product_id BETWEEN 11 AND 19;

INSERT INTO product_images (product_id, image_url, sort_order, is_main, created_at, updated_at) VALUES
(11, 'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/top7_%E1%84%8E%E1%85%A1%E1%86%A8%E1%84%8B%E1%85%AD%E1%86%BC%E1%84%89%E1%85%A3%E1%86%BA.jpg',     1, true,  NOW(), NOW()),
(11, 'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/top7_%E1%84%83%E1%85%A1%E1%86%AB%E1%84%83%E1%85%A9%E1%86%A8%E1%84%89%E1%85%A3%E1%86%BA.heic',    2, false, NOW(), NOW()),
(12, 'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/top8_%E1%84%8E%E1%85%A1%E1%86%A8%E1%84%8B%E1%85%AD%E1%86%BC%E1%84%89%E1%85%A3%E1%86%BA.jpg',     1, true,  NOW(), NOW()),
(12, 'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/top8_%E1%84%83%E1%85%A1%E1%86%AB%E1%84%83%E1%85%A9%E1%86%A8%E1%84%89%E1%85%A3%E1%86%BA.heic',    2, false, NOW(), NOW()),
(13, 'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/top9_%E1%84%8E%E1%85%A1%E1%86%A8%E1%84%8B%E1%85%AD%E1%86%BC%E1%84%89%E1%85%A3%E1%86%BA.jpg',     1, true,  NOW(), NOW()),
(13, 'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/top9_%E1%84%83%E1%85%A1%E1%86%AB%E1%84%83%E1%85%A9%E1%86%A8%E1%84%89%E1%85%A3%E1%86%BA.heic',    2, false, NOW(), NOW()),
(14, 'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/top10_%E1%84%8E%E1%85%A1%E1%86%A8%E1%84%8B%E1%85%AD%E1%86%BC%E1%84%89%E1%85%A3%E1%86%BA.jpg',    1, true,  NOW(), NOW()),
(14, 'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/top10_%E1%84%83%E1%85%A1%E1%86%AB%E1%84%83%E1%85%A9%E1%86%A8%E1%84%89%E1%85%A3%E1%86%BA.heic',   2, false, NOW(), NOW()),
(15, 'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/top11_%E1%84%83%E1%85%A1%E1%86%AB%E1%84%83%E1%85%A9%E1%86%A8%E1%84%89%E1%85%A3%E1%86%BA.heic',   1, true,  NOW(), NOW()),
(16, 'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/bottom3_%E1%84%83%E1%85%A1%E1%86%AB%E1%84%83%E1%85%A9%E1%86%A8%E1%84%89%E1%85%A3%E1%86%BA.heic', 1, true,  NOW(), NOW()),
(17, 'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/bottom4_%E1%84%83%E1%85%A1%E1%86%AB%E1%84%83%E1%85%A9%E1%86%A8%E1%84%89%E1%85%A3%E1%86%BA.heic', 1, true,  NOW(), NOW()),
(18, 'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/acc1_%E1%84%83%E1%85%A1%E1%86%AB%E1%84%83%E1%85%A9%E1%86%A8%E1%84%89%E1%85%A3%E1%86%BA.heic',    1, true,  NOW(), NOW()),
(19, 'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/acc2_%E1%84%83%E1%85%A1%E1%86%AB%E1%84%83%E1%85%A9%E1%86%A8%E1%84%89%E1%85%A3%E1%86%BA.heic',    1, true,  NOW(), NOW());

-- =====================================================
-- 5) 시퀀스 보정 (명시 ID INSERT 후)
-- =====================================================

SELECT setval(pg_get_serial_sequence('products', 'product_id'),
              GREATEST((SELECT MAX(product_id) FROM products), 1));

SELECT setval(pg_get_serial_sequence('product_images', 'product_img_id'),
              GREATEST((SELECT MAX(product_img_id) FROM product_images), 1));

COMMIT;

-- 검증 쿼리 (참고용, 위 트랜잭션과 별개)
-- SELECT product_id, product_name, category_code, price, condition_code FROM products ORDER BY product_id;
-- SELECT product_id, COUNT(*) AS img_count FROM product_images GROUP BY product_id ORDER BY product_id;
