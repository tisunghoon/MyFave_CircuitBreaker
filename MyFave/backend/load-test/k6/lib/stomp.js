// STOMP frame 빌더 — k6/ws는 raw WebSocket만 지원하므로 STOMP 프로토콜은 수동으로 직조.
// 백엔드: spring-boot-starter-websocket + @MessageMapping. 자세한 경로는 시나리오 E 참조.

import { LOADTEST_RUN_ID, LOADTEST_SCENARIO, LOADTEST_ROUND } from './context.js';

// STOMP frame 종료자: NULL byte (0x00). String.fromCharCode로 명시적으로 생성.
const NULL_BYTE = String.fromCharCode(0);

function loadtestStompHeaders() {
  const lines = [];
  if (LOADTEST_RUN_ID) lines.push(`X-Loadtest-Run-Id:${LOADTEST_RUN_ID}`);
  if (LOADTEST_SCENARIO) lines.push(`X-Loadtest-Scenario:${LOADTEST_SCENARIO}`);
  if (LOADTEST_ROUND) lines.push(`X-Loadtest-Round:${LOADTEST_ROUND}`);
  return lines.length > 0 ? lines.join('\n') + '\n' : '';
}

export function connect(accessToken) {
  // Bearer 토큰은 JwtChannelInterceptor가 STOMP 헤더에서 읽음.
  // 부하테스트 헤더는 JwtHandshakeInterceptor/ChannelInterceptor에서 MDC로 옮길 수 있음.
  return (
    'CONNECT\n' +
    'accept-version:1.2\n' +
    'host:localhost\n' +
    `Authorization:Bearer ${accessToken}\n` +
    loadtestStompHeaders() +
    '\n' +
    NULL_BYTE
  );
}

export function subscribe(id, destination) {
  return (
    'SUBSCRIBE\n' +
    `id:${id}\n` +
    `destination:${destination}\n` +
    '\n' +
    NULL_BYTE
  );
}

export function send(destination, body, contentType = 'application/json') {
  const payload = typeof body === 'string' ? body : JSON.stringify(body);
  return (
    'SEND\n' +
    `destination:${destination}\n` +
    `content-type:${contentType}\n` +
    `content-length:${payload.length}\n` +
    '\n' +
    payload +
    NULL_BYTE
  );
}

export function disconnect() {
  return 'DISCONNECT\n\n' + NULL_BYTE;
}

// 수신 메시지에서 STOMP 명령(MESSAGE/CONNECTED/ERROR) 종류 추출
export function frameCommand(raw) {
  if (typeof raw !== 'string') return null;
  const newlineIdx = raw.indexOf('\n');
  return newlineIdx > 0 ? raw.substring(0, newlineIdx) : raw;
}
