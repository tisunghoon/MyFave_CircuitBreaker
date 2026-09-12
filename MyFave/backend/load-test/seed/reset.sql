-- 라운드 사이 환경 리셋 — k6 실행 직전 매번 실행.
-- 트랜잭션 테이블 비우고 상품 10개의 stock_quantity를 1로 되돌린다.

BEGIN;

-- 결제 / 주문 / 장바구니 초기화 (FK 순서 고려)
TRUNCATE TABLE payments, order_items, orders, cart_items RESTART IDENTITY CASCADE;

-- 시나리오 D 검증을 위해 상품 10개 모두 재고 1, 비-품절로 리셋
UPDATE products
   SET stock_quantity = 1,
       is_soldout = false
 WHERE product_id BETWEEN 1 AND 10;

-- 통계 갱신 (Round 1 인덱스 적용 후 효과 측정 시 필수)
ANALYZE products;
ANALYZE orders;
ANALYZE order_items;

COMMIT;
