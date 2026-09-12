# MyFave Load Test (k6)

5/25 라이브 이벤트 대비 부하 테스트. **D(성훈) 담당** 영역.

상세 설명 / 통과 기준 / 라운드별 비교 표는 노션:
- 실행 계획 — https://www.notion.so/36704552316d802fab3cc1ea7ed818cc
- 검증 방법 — https://www.notion.so/36704552316d802f93d4da55fc3ee8a3

## 디렉토리

```
backend/load-test/
├── k6/
│   ├── scenarios/   # 시나리오 3종 (A/D/E)
│   ├── lib/         # 공통 헬퍼 (http, pool, stomp)
│   └── data/        # tokens.json (gitignored)
├── seed/
│   ├── token-prepare.mjs   # Node 스크립트 — 1000 토큰 사전 생성
│   └── reset.sql           # 라운드 사이 환경 리셋
└── docs/
    └── ws-metrics-contract.md  # C 작업자(Micrometer) 인터페이스
```

## 사전 준비 (한 번만)

```bash
# 1. 인프라
docker-compose up -d
docker-compose -f docker-compose.monitoring.yml up -d

# 2. 백엔드 (loadtest 프로파일 — 1000 유저/Shipping/SaleEvent/ChatRoom 자동 시드)
cd backend
SPRING_PROFILES_ACTIVE=loadtest ./gradlew bootRun

# 3. 토큰 풀 (별도 터미널)
node backend/load-test/seed/token-prepare.mjs
# → backend/load-test/k6/data/tokens.json 생성
```

## 라운드 사이클

```bash
# 매 라운드마다
psql -h localhost -U postgres -d myfave -f backend/load-test/seed/reset.sql

# 시나리오 실행 (택1)
k6 run backend/load-test/k6/scenarios/scenario-a-spike.js
k6 run backend/load-test/k6/scenarios/scenario-d-soldout.js
k6 run backend/load-test/k6/scenarios/scenario-e-chat.js
```

## 시나리오 D 후처리 검증

```sql
-- PAID 카운트 (기대: 10)
SELECT COUNT(*) FROM order_items oi
  JOIN orders o ON oi.order_id = o.order_id
 WHERE o.order_status = 'PAID';

-- 재고 0 확인
SELECT product_id, stock_quantity, is_soldout
  FROM products WHERE product_id BETWEEN 1 AND 10;

-- 동일 상품 over-selling 검출 (기대: 0행)
SELECT product_id, COUNT(*)
  FROM order_items oi JOIN orders o ON oi.order_id = o.order_id
 WHERE o.order_status = 'PAID'
 GROUP BY product_id HAVING COUNT(*) > 1;
```

## 결과 기록 표

| Round | 시나리오 | TPS | p50 | p95 | p99 | 에러율 | DB conn peak | CPU peak | 비고 |
|---|---|---|---|---|---|---|---|---|---|
| 0 | A |   |   |   |   |   |   |   | 베이스라인 |
| 0 | D |   |   |   |   |   |   |   | over-selling 0건 |
| 0 | E |   |   |   |   |   |   |   | sessions=1000 |
| 1 | A |   |   |   |   |   |   |   | 인덱스 적용 |
| 1 | D |   |   |   |   |   |   |   |   |
| 1 | E |   |   |   |   |   |   |   |   |
| 2 | A/D/E | ... | ... | ... | ... | ... | ... | ... | N+1 제거 |
| 3 | A/D/E | ... | ... | ... | ... | ... | ... | ... | 캐싱 |

각 라운드 측정 후 노션에 동기화.

## 의존성

- A(현영) EC2: 의존 없음 (로컬에서 측정)
- B(항중) Prometheus/Loki/Tempo: `docker-compose.monitoring.yml` 기동 필요
- C(정수) Grafana + Micrometer: 시나리오 E는 `docs/ws-metrics-contract.md` 메트릭 적용 후 측정
