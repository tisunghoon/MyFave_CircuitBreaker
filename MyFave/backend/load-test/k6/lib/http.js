// HTTP 헬퍼 — baseURL/헤더 합치는 잡일을 한 곳에 모아둠.
// 시나리오 코드를 깔끔하게 유지하기 위함.

import http from 'k6/http';
import { loadtestHeaders } from './context.js';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';

export function authHeaders(token, extra = {}) {
  return {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      // 부하테스트 Round/Scenario/RunId 헤더 — 모든 HTTP 요청에 자동 부착
      ...loadtestHeaders(),
      ...extra,
    },
  };
}

export function get(path, token, params = {}) {
  return http.get(`${BASE_URL}${path}`, {
    ...authHeaders(token),
    ...params,
  });
}

export function post(path, token, body, params = {}) {
  return http.post(`${BASE_URL}${path}`, JSON.stringify(body), {
    ...authHeaders(token),
    ...params,
  });
}

// 상품 10개 중 VU 인덱스 기반으로 productId 매핑 (시나리오 D 매진 경쟁용 — 동일 상품에 30명씩 몰림)
export function pickProductIdByVu(vuId) {
  return ((vuId - 1) % 10) + 1;
}
