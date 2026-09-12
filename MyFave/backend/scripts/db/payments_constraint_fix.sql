-- ============================================================================
-- payments_constraint_fix.sql
-- ----------------------------------------------------------------------------
-- 목적:
--   payments 테이블의 옛 CHECK 제약(CREDITCARD/TOSS/COMPLETE)을 현재 Java
--   enum(PaymentMethod, PaymentStatus) 값에 맞춰 보정한다.
--
-- 선행 조건 (반드시 확인 후 실행):
--   1) payments_constraint_diagnose.sql 을 먼저 실행했고,
--      - Section 1: CHECK 정의에 'CREDITCARD' 또는 'TOSS' 또는 'COMPLETE' 가
--        포함되어 있거나
--      - Section 3: legacy_method_creditcard / legacy_method_toss /
--        legacy_status_complete 중 1건 이상 존재
--      → 둘 다 0 이면 본 스크립트는 실행할 필요 없다.
--   2) 운영 DB 전체 백업(스냅샷 또는 pg_dump)을 완료했다.
--   3) 결제 트래픽이 일시 중단되어도 무방한 시점이다 (CHECK DROP/ADD 사이의
--      찰나에 INSERT 가 들어오면 일시적으로 검증이 느슨해지므로 트랜잭션으로 묶음).
--
-- 실행 방법 (반드시 인터랙티브 psql 세션에서):
--   1) psql -h <host> -U <user> -d <db>
--   2) BEGIN;
--   3) \i /path/to/payments_constraint_fix.sql
--   4) Section 4 출력 검토
--   5) 정상이면 COMMIT;  /  문제가 보이면 ROLLBACK;
--   ※ `psql -f payments_constraint_fix.sql` 단발 실행은 금지.
--     세션 종료 시 자동 ROLLBACK 되거나(스크립트에 COMMIT 없음 → 변경 유실)
--     단계별 검토 없이 statement-by-statement 자동 커밋되어
--     ALTER 후 검증 단계의 의미가 사라진다.
--
-- 트랜잭션 안전:
--   본 스크립트 자체에는 BEGIN/COMMIT 이 포함되어 있지 않다.
--   호출자가 인터랙티브 psql 세션에서 BEGIN; 으로 트랜잭션을 연 뒤
--   `\i` 로 본 스크립트를 로드하고, Section 4 검증 결과를 본 다음
--   수동으로 COMMIT; 또는 ROLLBACK; 을 입력해야 한다.
--
-- 참고:
--   - ALTER SQL 본문은 노션 "결제 도메인 DB CHECK 제약과 enum 값 불일치 이슈"
--     의 제안 해결책을 그대로 인용한다.
--   - 코드 측 enum 정의 (변경 시 반드시 본 스크립트도 함께 갱신):
--     backend/src/main/java/com/myfave/api/domain/payment/entity/PaymentMethod.java
--     backend/src/main/java/com/myfave/api/domain/payment/entity/PaymentStatus.java
-- ============================================================================


-- Step 1. 옛 값 → 신 값 데이터 마이그레이션
\echo '=== [1/4] migrating legacy values to current enum values ==='
UPDATE payments SET payment_method = 'CARD'      WHERE payment_method = 'CREDITCARD';
UPDATE payments SET payment_method = 'TOSS_PAY'  WHERE payment_method = 'TOSS';
UPDATE payments SET payment_status = 'COMPLETED' WHERE payment_status = 'COMPLETE';


-- Step 2. payment_method CHECK 제약 교체
\echo ''
\echo '=== [2/4] replacing payments_payment_method_check ==='
ALTER TABLE payments DROP CONSTRAINT payments_payment_method_check;
ALTER TABLE payments ADD CONSTRAINT payments_payment_method_check
  CHECK (payment_method IN ('CARD', 'TOSS_PAY', 'KAKAO_PAY', 'NAVER_PAY'));


-- Step 3. payment_status CHECK 제약 교체
\echo ''
\echo '=== [3/4] replacing payments_payment_status_check ==='
ALTER TABLE payments DROP CONSTRAINT payments_payment_status_check;
ALTER TABLE payments ADD CONSTRAINT payments_payment_status_check
  CHECK (payment_status IN (
    'PENDING', 'AUTHORIZED', 'COMPLETED',
    'FAILED', 'CANCELLED', 'PARTIAL_CANCELLED'
  ));


-- Step 4. 사후 검증 — 새 CHECK 정의를 재조회해 의도대로 적용됐는지 확인
\echo ''
\echo '=== [4/4] post-check: payments CHECK constraints ==='
SELECT con.conname,
       pg_get_constraintdef(con.oid) AS definition
FROM   pg_constraint con
JOIN   pg_class      rel ON rel.oid = con.conrelid
WHERE  rel.relname = 'payments'
  AND  con.contype = 'c'
ORDER  BY con.conname;


-- ---------------------------------------------------------------------------
-- 위 검증 결과를 확인한 뒤, 인터랙티브 세션에서 직접 입력한다:
--   정상  → COMMIT;
--   이상  → ROLLBACK;
-- ---------------------------------------------------------------------------
