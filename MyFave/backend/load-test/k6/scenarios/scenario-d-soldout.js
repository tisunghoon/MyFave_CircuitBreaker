// 시나리오 D — 한정 수량 매진 경쟁 (Must)
// 상품 10개 × stock 1 × VU 300 → 정확히 10명만 성공, 나머지 290명 PRODUCT_SOLD_OUT.
//
// 실행 전: psql -f backend/load-test/seed/reset.sql 로 재고 1 리셋
// 실행 후 검증: backend/load-test/README.md 의 SQL 3종

import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';

import { get, post, pickProductIdByVu, BASE_URL } from '../lib/http.js';
import { tokens } from '../lib/pool.js';

export const options = {
  scenarios: {
    soldout: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 50 },
        { duration: '5s', target: 300 },
        { duration: '30s', target: 300 },
      ],
      gracefulRampDown: '5s',
    },
  },
  thresholds: {
    // over-selling 0건 — 한 VU가 1회만 성공해야 정상. 누적 성공 11건 이상이면 시스템 버그.
    soldout_confirm_success: ['count<=10'],
    soldout_oversell_detected: ['count==0'],
  },
};

const orderCreated = new Counter('soldout_order_created');
const orderFailedSoldOut = new Counter('soldout_order_failed_soldout');
const orderFailedOther = new Counter('soldout_order_failed_other');
const paymentConfirmed = new Counter('soldout_confirm_success');
const oversellDetected = new Counter('soldout_oversell_detected');
const errorRate = new Rate('soldout_error_rate');

export function setup() {
  console.log(`[scenario-D] BASE_URL=${BASE_URL}, 토큰 풀=${tokens.length} (앞 300명 사용)`);
  if (tokens.length < 300) {
    console.warn(`[scenario-D] 토큰이 ${tokens.length}개뿐. 300명 시나리오를 다 못 채울 수 있음`);
  }
}

export default function () {
  const token = tokens[(__VU - 1) % tokens.length];
  const accessToken = token.accessToken;
  const shippingAddressId = token.shippingAddressId;
  // VU 1~300을 상품 1~10 에 30명씩 분배 → 각 상품에 30명이 경쟁
  const productId = pickProductIdByVu(__VU);

  // 1) 상품 조회
  const productResp = get(`/products/${productId}`, accessToken);
  if (!check(productResp, { 'product 200': (r) => r.status === 200 })) {
    errorRate.add(1);
    return;
  }

  // 2) 주문 생성 — DIRECT
  const orderResp = post('/orders', accessToken, {
    orderType: 'DIRECT',
    productId,
    shippingAddressId,
  });

  if (orderResp.status === 200 || orderResp.status === 201) {
    orderCreated.add(1);
  } else {
    errorRate.add(1);
    const body = safeBody(orderResp);
    if (body?.code === 'PRODUCT_SOLD_OUT' || /품절/.test(body?.message ?? '')) {
      orderFailedSoldOut.add(1);
    } else {
      orderFailedOther.add(1, { status: String(orderResp.status) });
    }
    return; // 주문 실패하면 결제까지 안 감
  }

  const orderId = parseOrderId(orderResp);
  if (!orderId) return;

  // 3) 결제 준비
  const prepareResp = post('/payments/prepare', accessToken, { orderId });
  if (prepareResp.status !== 200 && prepareResp.status !== 201) {
    errorRate.add(1);
    return;
  }

  const paymentId = parsePaymentId(prepareResp);

  // 4) 결제 확인
  const confirmResp = post('/payments/confirm', accessToken, {
    paymentId,
    pgTransactionId: `loadtest-tx-${__VU}-${__ITER}`,
  });
  if (confirmResp.status === 200 || confirmResp.status === 201) {
    paymentConfirmed.add(1, { productId: String(productId) });
  } else {
    errorRate.add(1);
  }

  sleep(0.1);
}

function safeBody(res) {
  try {
    return res.json();
  } catch (_e) {
    return null;
  }
}

function parseOrderId(res) {
  const body = safeBody(res);
  if (!body) return null;
  const data = body.data ?? body;
  return data.orderId ?? data.id ?? null;
}

function parsePaymentId(res) {
  const body = safeBody(res);
  if (!body) return null;
  const data = body.data ?? body;
  return data.paymentId ?? data.id ?? null;
}

// 최종 검증 메시지 — k6 콘솔에 한 줄 요약 출력
export function teardown() {
  console.log(
    '[scenario-D] 결과 요약 — DB query 로 추가 검증 필수: \n' +
      '  1) SELECT COUNT(*) FROM order_items WHERE ... PAID == 10 ?\n' +
      '  2) SELECT stock_quantity FROM products WHERE product_id<=10 == 0 ?\n' +
      '  3) 동일 product_id 로 PAID order_items 2건 이상 0행 ?'
  );
}
