#!/usr/bin/env node
// 부하 테스트 시작 전 1회 실행 — loadtest 프로파일 백엔드가 떠 있고 시드 1000 유저가 박혀 있어야 함.
//
// 동작:
//   1. loadtest{1..N}@myfave.test 계정으로 로그인 → accessToken/refreshToken/userId 수집
//   2. 각 유저의 default ShippingAddress id 조회 (GET /shipping)
//   3. 결과를 ../k6/data/tokens.json 으로 저장 (k6 SharedArray가 읽음)
//
// 사용:
//   node backend/load-test/seed/token-prepare.mjs [--count 1000] [--base http://localhost:8080/api/v1]

import { writeFile, mkdir } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const OUTPUT = join(__dirname, '..', 'k6', 'data', 'tokens.json');

const args = parseArgs(process.argv.slice(2));
const BASE_URL = args.base ?? process.env.BASE_URL ?? 'http://localhost:8080/api/v1';
const COUNT = Number(args.count ?? process.env.COUNT ?? 1000);
const PASSWORD = args.password ?? process.env.LOADTEST_PASSWORD ?? 'Password123!';
const CONCURRENCY = Number(args.concurrency ?? 50);

console.log(`[token-prepare] BASE_URL=${BASE_URL} COUNT=${COUNT} CONCURRENCY=${CONCURRENCY}`);

const indices = Array.from({ length: COUNT }, (_, i) => i + 1);
const results = new Array(COUNT);
const failures = [];

let nextIdx = 0;
const start = Date.now();

async function worker(workerId) {
  while (true) {
    const i = nextIdx++;
    if (i >= indices.length) return;
    const n = indices[i];
    try {
      const entry = await prepareOne(n);
      results[i] = entry;
      if ((i + 1) % 100 === 0) {
        console.log(`[token-prepare] ${i + 1}/${COUNT} 완료`);
      }
    } catch (err) {
      failures.push({ n, error: err.message });
    }
  }
}

async function prepareOne(n) {
  const loginResp = await fetch(`${BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      email: `loadtest${n}@myfave.test`,
      password: PASSWORD,
    }),
  });
  if (!loginResp.ok) {
    throw new Error(`login ${n} HTTP ${loginResp.status}`);
  }
  const loginBody = await loginResp.json();
  const data = loginBody.data ?? loginBody;
  const accessToken = data.accessToken;
  const userId = data.userId;
  const refreshToken = data.refreshToken;

  const shipResp = await fetch(`${BASE_URL}/shipping`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  let shippingAddressId = null;
  if (shipResp.ok) {
    const shipBody = await shipResp.json();
    const list = shipBody.data ?? shipBody;
    const items = Array.isArray(list) ? list : (list.items ?? list.content ?? []);
    const def = items.find((it) => it.isDefault || it.default) ?? items[0];
    shippingAddressId = def?.shippingId ?? def?.id ?? null;
  }

  return { userId, accessToken, refreshToken, shippingAddressId };
}

await Promise.all(
  Array.from({ length: CONCURRENCY }, (_, k) => worker(k))
);

const filled = results.filter(Boolean);
console.log(
  `[token-prepare] 성공 ${filled.length}/${COUNT}, 실패 ${failures.length}, ${(Date.now() - start) / 1000}s`
);
if (failures.length > 0) {
  console.log('[token-prepare] 실패 샘플:', failures.slice(0, 5));
}

await mkdir(dirname(OUTPUT), { recursive: true });
await writeFile(OUTPUT, JSON.stringify(filled, null, 0));
console.log(`[token-prepare] 저장 완료 → ${OUTPUT}`);

function parseArgs(argv) {
  const out = {};
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a.startsWith('--')) {
      const key = a.slice(2);
      const val = argv[i + 1];
      out[key] = val;
      i++;
    }
  }
  return out;
}
