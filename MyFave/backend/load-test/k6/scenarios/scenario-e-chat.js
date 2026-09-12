// 시나리오 E — WebSocket 채팅 1000 (Should)
// 30s→500 / 3m→1000 / 1m→1000 동시 STOMP 세션.
// 모든 VU: CONNECT → SUBSCRIBE /topic/chat/{roomId}
// 5% VU: 3~7초 간격으로 SEND /app/chat/{roomId} 발행
// 인플루언서(VU=1): 10초마다 broadcast
//
// 측정 지표(Grafana 에서 확인 — C 작업자 메트릭):
//   - myfave.websocket.sessions.active (Gauge)
//   - myfave.chat.broadcast.duration (Timer)
//   - myfave.chat.ratelimit.rejected (Counter)

import ws from 'k6/ws';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';

import { connect, subscribe, send, disconnect, frameCommand } from '../lib/stomp.js';
import { tokens } from '../lib/pool.js';

const WS_BASE = __ENV.WS_URL || 'ws://localhost:8080/ws';
const ROOM_ID = __ENV.ROOM_ID || '1';

function sockJsWsUrl(base) {
  const server = String(Math.floor(Math.random() * 999)).padStart(3, '0');
  const session = Array.from({length: 8}, () => Math.random().toString(36)[2]).join('');
  return `${base}/${server}/${session}/websocket`;
}
const SESSION_DURATION_MS = 4 * 60 * 1000 + 30 * 1000; // 4분 30초

export const options = {
  scenarios: {
    chat: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 500 },
        { duration: '3m', target: 1000 },
        { duration: '1m', target: 1000 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    chat_connect_failed: ['count<10'],          // 1000명 중 10명 미만 실패
    chat_messages_received: ['count>0'],
  },
};

const connectFailed = new Counter('chat_connect_failed');
const messagesReceived = new Counter('chat_messages_received');
const messagesSent = new Counter('chat_messages_sent');
const subscribeTime = new Trend('chat_subscribe_ms', true);

export function setup() {
  console.log(`[scenario-E] WS_BASE=${WS_BASE}, ROOM_ID=${ROOM_ID}, 토큰 풀=${tokens.length}`);
}

export default function () {
  const WS_URL = sockJsWsUrl(WS_BASE); // iteration마다 고유한 세션 ID 생성
  const token = tokens[(__VU - 1) % tokens.length];
  const accessToken = token.accessToken;

  const isInfluencer = __VU === 1;
  const isPublisher = isInfluencer || Math.random() < 0.05; // 5% 발행자

  const params = {
    headers: { Authorization: `Bearer ${accessToken}` },
  };

  const subscribeStart = Date.now();

  const res = ws.connect(WS_URL, params, function (socket) {
    let connected = false;
    let publishTimer = null;

    socket.on('open', () => {
      socket.send(connect(accessToken));
    });

    socket.on('message', (raw) => {
      const cmd = frameCommand(raw);
      if (cmd === 'CONNECTED' && !connected) {
        connected = true;
        socket.send(subscribe('sub-0', `/topic/chat/${ROOM_ID}`));
        subscribeTime.add(Date.now() - subscribeStart);

        if (isPublisher) {
          const intervalMs = isInfluencer ? 10_000 : randomBetween(3_000, 7_000);
          publishTimer = socket.setInterval(() => {
            const body = {
              type: 'SEND_MESSAGE',
              payload: {
                content: isInfluencer
                  ? `[host] live-msg-${Date.now()}`
                  : `[u${__VU}] hello-${__ITER}`,
              },
            };
            socket.send(send(`/app/chat/${ROOM_ID}`, body));
            messagesSent.add(1);
          }, intervalMs);
        }
      } else if (cmd === 'MESSAGE') {
        messagesReceived.add(1);
      } else if (cmd === 'ERROR') {
        connectFailed.add(1);
        socket.close();
      }
    });

    socket.on('error', () => {
      connectFailed.add(1);
    });

    socket.on('close', () => {
      if (publishTimer) socket.clearInterval(publishTimer);
    });

    socket.setTimeout(() => {
      socket.send(disconnect());
      socket.close();
    }, SESSION_DURATION_MS);
  });

  check(res, { 'ws handshake 101': (r) => r && r.status === 101 });
}

function randomBetween(min, max) {
  return Math.floor(Math.random() * (max - min) + min);
}
