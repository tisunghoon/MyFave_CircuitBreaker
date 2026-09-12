// 시나리오 A — 이벤트 오픈 스파이크 (Must)
// 30s→50 / 10s→1000 / 3m→1000 / 30s→0 의 폭증 패턴.
// 호출 mix: GET 30%, cart-add 20%, order 15%, payment-prepare 10%, coupons 15%, chat-preview 10%

import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

import { get, post, pickProductIdByVu, BASE_URL } from '../lib/http.js';
import { tokens } from '../lib/pool.js';

export const options = {
  scenarios: {
    spike: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 50 },
        { duration: '10s', target: 1000 },
        { duration: '3m', target: 1000 },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000'],
  },
};

const spikeWindow = new Trend('spike_first10s_latency', true);
const errorBuckets = new Counter('spike_errors_by_endpoint');

// 가중치 누적합 — pickEndpoint에서 사용
const ENDPOINTS = [
  { name: 'GET /products/{id}', weight: 30 },
  { name: 'POST /cart-items', weight: 20 },
  { name: 'POST /orders', weight: 15 },
  { name: 'POST /payments/prepare', weight: 10 },
  { name: 'GET /coupons', weight: 15 },
  { name: 'GET /chat-preview-fallback', weight: 10 },
];

function pickEndpoint() {
  const total = ENDPOINTS.reduce((s, e) => s + e.weight, 0);
  const r = Math.random() * total;
  let acc = 0;
  for (const e of ENDPOINTS) {
    acc += e.weight;
    if (r < acc) return e.name;
  }
  return ENDPOINTS[0].name;
}

export function setup() {
  console.log(`[scenario-A] BASE_URL=${BASE_URL}, 토큰 풀=${tokens.length}`);
}

export default function () {
  const token = tokens[(__VU - 1) % tokens.length];
  const accessToken = token.accessToken;
  const shippingAddressId = token.shippingAddressId;
  const productId = pickProductIdByVu(__VU + __ITER);
  const endpoint = pickEndpoint();

  const startedAt = Date.now();
  let res;

  switch (endpoint) {
    case 'GET /products/{id}':
      res = get(`/products/${productId}`, accessToken);
      break;
    case 'POST /cart-items':
      res = post('/cart-items', accessToken, { productId, quantity: 1 });
      break;
    case 'POST /orders':
      res = post('/orders', accessToken, {
        orderType: 'DIRECT',
        productId,
        shippingAddressId,
      });
      break;
    case 'POST /payments/prepare':
      // orderId 없이 호출 시 실패가 정상 — 부하만 발생시키기 위한 호출
      res = post('/payments/prepare', accessToken, { orderId: 0 });
      break;
    case 'GET /coupons':
      res = get('/coupons', accessToken);
      break;
    case 'GET /chat-preview-fallback':
      // chat preview 엔드포인트가 없으므로 products 페이지로 대체 (스펙 정의 시 교체)
      res = get('/products?page=0&size=10', accessToken);
      break;
  }

  const ok = check(res, { 'status < 500': (r) => r.status < 500 });
  if (!ok) errorBuckets.add(1, { endpoint });

  // 폭증 초기 10초 구간(40~50초)의 latency만 따로 추적
  const elapsed = (Date.now() - startedAt) / 1000;
  if (__ITER < 10) spikeWindow.add(elapsed * 1000);

  sleep(Math.random() * 0.5);
}
