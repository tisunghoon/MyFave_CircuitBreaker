// 토큰 풀 로더. token-prepare.mjs가 만들어둔 tokens.json을 k6 SharedArray에 한 번만 로드.
// 모든 VU가 같은 메모리 영역을 읽음 → 1000개 토큰을 메모리에 N번 복제하지 않음.

import { SharedArray } from 'k6/data';

export const tokens = new SharedArray('tokens', () => {
  // open()은 init context에서만 동작. k6 워크플로상 자동 호출됨.
  const raw = open('../data/tokens.json');
  const parsed = JSON.parse(raw);
  if (!Array.isArray(parsed) || parsed.length === 0) {
    throw new Error('tokens.json이 비어 있습니다. seed/token-prepare.mjs를 먼저 실행하세요.');
  }
  return parsed;
});

// VU 번호 기반 라운드로빈 (1-indexed).
export function pickByVu(vuId) {
  return tokens[(vuId - 1) % tokens.length];
}

// 시나리오 D 처럼 앞 N명만 쓸 때 사용.
export function slice(n) {
  return tokens.slice(0, n);
}
