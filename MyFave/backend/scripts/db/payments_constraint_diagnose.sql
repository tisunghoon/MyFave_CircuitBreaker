-- ============================================================================
-- payments_constraint_diagnose.sql
-- ----------------------------------------------------------------------------
-- 목적:
--   payments 테이블의 CHECK 제약과 데이터가 현재 Java enum
--   (PaymentMethod, PaymentStatus) 정의와 일치하는지 진단한다.
--
-- 배경:
--   과거 일부 환경의 DB에는 옛 값(CREDITCARD/TOSS/COMPLETE) 기반의 CHECK 제약이
--   박혀 있어 코드 측 enum(CARD/TOSS_PAY/COMPLETED) 으로 INSERT 시
--   constraint violation(500) 이 발생했다.
--   상세 분석: 노션 "결제 도메인 DB CHECK 제약과 enum 값 불일치 이슈"
--
-- 실행 방법:
--   psql -h <host> -U <user> -d <db> -f payments_constraint_diagnose.sql
--   또는
--   docker exec -i <postgres_container> psql -U postgres -d myfave \
--     -f /path/to/payments_constraint_diagnose.sql
--
-- 다음 단계:
--   1) Section 1 출력의 CHECK 정의에 'CREDITCARD' / 'TOSS' / 'COMPLETE' 가
--      포함되어 있거나
--   2) Section 3 출력의 옛 값 row 가 1건 이상이면
--   → payments_constraint_fix.sql 을 실행해 보정한다.
--   둘 다 0 이면 별도 조치 불필요.
--
-- 본 스크립트는 read-only 다. 데이터/스키마를 변경하지 않는다.
-- ============================================================================


-- Section 1. 현재 CHECK 제약 정의
\echo '=== [1/3] payments CHECK constraints ==='
SELECT con.conname,
       pg_get_constraintdef(con.oid) AS definition
FROM   pg_constraint con
JOIN   pg_class      rel ON rel.oid = con.conrelid
WHERE  rel.relname = 'payments'
  AND  con.contype = 'c'
ORDER  BY con.conname;


-- Section 2. 결제수단/상태별 row 통계 (현재 데이터 분포)
\echo ''
\echo '=== [2/3] payments row count by (payment_method, payment_status) ==='
SELECT payment_method,
       payment_status,
       COUNT(*) AS row_count
FROM   payments
GROUP  BY payment_method, payment_status
ORDER  BY payment_method, payment_status;


-- Section 3. 옛 값(CREDITCARD / TOSS / COMPLETE) 잔존 카운트
--   → 1건 이상이면 payments_constraint_fix.sql 의 UPDATE 단계가 필요하다.
\echo ''
\echo '=== [3/3] legacy value count (CREDITCARD / TOSS / COMPLETE) ==='
SELECT
  SUM(CASE WHEN payment_method = 'CREDITCARD' THEN 1 ELSE 0 END) AS legacy_method_creditcard,
  SUM(CASE WHEN payment_method = 'TOSS'       THEN 1 ELSE 0 END) AS legacy_method_toss,
  SUM(CASE WHEN payment_status = 'COMPLETE'   THEN 1 ELSE 0 END) AS legacy_status_complete
FROM payments;
