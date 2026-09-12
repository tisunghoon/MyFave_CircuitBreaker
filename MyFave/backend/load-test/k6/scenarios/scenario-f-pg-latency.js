// 시나리오 F — 외부 PG 지연 캐스케이드
// 결제 confirm + 상품목록/주문조회를 동시에 때린다. chaos ON 시 결제 경로가
// 공유 톰캣 스레드풀·HikariCP 커넥션을 먹어 "읽기 경로가 먼저 죽는" 순간을 만든다.
//
// 실행:
//   1) SPRING_PROFILES_ACTIVE=local,chaos ./gradlew bootRun
//   2) k6 run backend/load-test/k6/scenarios/scenario-f-pg-latency.js
//   3) curl -X POST 'localhost:8080/api/v1/internal/chaos/pg?latencyMs=8000&enabled=true'
//   4) Grafana 에서 상품목록 p99 / hikari pending / PG p99 관측

import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

import { get, post, pickProductIdByVu, BASE_URL } from '../lib/http.js';
import { tokens } from '../lib/pool.js';

export const options = {
  scenarios: {
    // 결제 경로 — 소수 VU 로 confirm 반복 (chaos ON 시 스레드/커넥션 점유)
    payment: { executor: 'constant-vus', vus: 10, duration: '3m', exec: 'payFlow' },
    // 읽기 경로 — 결제와 무관한 조회. 결제가 자원을 먹으면 이쪽이 먼저 느려진다.
    read:    { executor: 'constant-vus', vus: 20, duration: '3m', exec: 'readFlow' },
  },
};

const confirmLatency     = new Trend('drill_confirm_latency', true);
const productListLatency = new Trend('drill_productlist_latency', true);
const orderListLatency   = new Trend('drill_orderlist_latency', true);
const readErrors         = new Counter('drill_read_errors');

export function setup() {
  console.log(`[scenario-F] BASE_URL=${BASE_URL}, 토큰 풀=${tokens.length}`);
  console.log('[scenario-F] chaos 토글: POST /api/v1/internal/chaos/pg?latencyMs=8000&enabled=true');
}

// 결제 흐름 — order 생성 → prepare → confirm(MOCK-PAY-{paymentId})
export function payFlow() {
  const token = tokens[(__VU - 1) % tokens.length];
  const accessToken = token.accessToken;
  const productId = pickProductIdByVu(__VU);

  const orderResp = post('/orders', accessToken, {
    orderType: 'DIRECT', productId, shippingAddressId: token.shippingAddressId,
  });
  if (orderResp.status !== 200 && orderResp.status !== 201) { sleep(0.5); return; }
  const orderId = parseId(orderResp, 'orderId');
  if (!orderId) return;

  const prepareResp = post('/payments/prepare', accessToken, { orderId });
  if (prepareResp.status !== 200 && prepareResp.status !== 201) { sleep(0.5); return; }
  const paymentId = parseId(prepareResp, 'paymentId');
  if (!paymentId) return;

  const confirmResp = post('/payments/confirm', accessToken, {
    paymentId, pgTransactionId: `MOCK-PAY-${paymentId}`,
  });
  confirmLatency.add(confirmResp.timings.duration);
  sleep(0.3);
}

// 읽기 흐름 — 상품목록 + 주문조회. 결제와 스레드풀을 공유하므로 캐스케이드의 피해자.
export function readFlow() {
  const token = tokens[(__VU - 1) % tokens.length];
  const accessToken = token.accessToken;

  const listResp = get('/products', accessToken);
  productListLatency.add(listResp.timings.duration);
  if (!check(listResp, { 'products 200': (r) => r.status === 200 })) readErrors.add(1);

  const orderResp = get('/orders', accessToken);
  orderListLatency.add(orderResp.timings.duration);
  if (!check(orderResp, { 'orders 200': (r) => r.status === 200 })) readErrors.add(1);

  sleep(0.2);
}

function parseId(res, key) {
  try {
    const body = res.json();
    const data = body.data ?? body;
    return data[key] ?? data.id ?? null;
  } catch (_e) { return null; }
}

export function teardown() {
  console.log('[scenario-F] 관측 포인트: 상품목록 p99(먼저 상승) / hikari pending / PG p99 / tomcat busy');
}
