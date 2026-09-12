// 부하테스트 라운드/시나리오 라벨 — 모든 HTTP/STOMP 요청 + k6 메트릭에 자동 부착.
// 백엔드 TraceIdFilter가 X-Loadtest-* 헤더를 MDC에 주입 → 로그/Loki 라벨로 사용.
// k6 자체 메트릭 (k6_http_req_duration_seconds, k6_vus 등)도 tags로 라운드 비교 가능.
//
// 환경변수 (k6 run --env 또는 셸 export):
//   LOADTEST_RUN_ID    예) 2026-05-25T18:00-D-r0   (라운드별 고유 식별)
//   LOADTEST_SCENARIO  예) A | B | D | E
//   LOADTEST_ROUND     예) 0 | 1 | 2 | 3

export const LOADTEST_RUN_ID = __ENV.LOADTEST_RUN_ID || '';
export const LOADTEST_SCENARIO = __ENV.LOADTEST_SCENARIO || '';
export const LOADTEST_ROUND = __ENV.LOADTEST_ROUND || '';

// HTTP 요청 헤더로 부착할 객체. http.js의 authHeaders에서 자동 머지.
export function loadtestHeaders() {
  const headers = {};
  if (LOADTEST_RUN_ID) headers['X-Loadtest-Run-Id'] = LOADTEST_RUN_ID;
  if (LOADTEST_SCENARIO) headers['X-Loadtest-Scenario'] = LOADTEST_SCENARIO;
  if (LOADTEST_ROUND) headers['X-Loadtest-Round'] = LOADTEST_ROUND;
  return headers;
}

// k6 options.tags에 머지하면 모든 k6 메트릭에 라벨 부착됨.
// 사용: export const options = { tags: { ...loadtestTags() }, scenarios: {...} };
// → Grafana myfave-loadtest-rounds 대시보드의 $scenario/$round 변수에서 사용.
export function loadtestTags() {
  const tags = {};
  if (LOADTEST_RUN_ID) tags.run_id = LOADTEST_RUN_ID;
  if (LOADTEST_SCENARIO) tags.scenario = LOADTEST_SCENARIO;
  if (LOADTEST_ROUND) tags.round = LOADTEST_ROUND;
  return tags;
}
