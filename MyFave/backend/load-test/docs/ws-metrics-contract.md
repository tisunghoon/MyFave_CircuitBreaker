# WebSocket / 채팅 메트릭 명세 (D → C 인터페이스)

D 작업자(k6 시나리오 E)가 검증할 지표를 정의합니다. **구현은 C 작업자(Actuator/Micrometer)** 가 진행합니다. 이름·태그·위치만 합의되면 D는 명세에 맞춰 시나리오를 작성하고 Grafana 패널을 검수합니다.

## Micrometer 메트릭 4종

| 메트릭 이름 | 타입 | 태그 | 추가 위치 (제안) | 용도 |
|---|---|---|---|---|
| `myfave.websocket.sessions.active` | Gauge | - | `SessionRegistry` Bean (또는 STOMP 이벤트 리스너) | 시나리오 E: 동시 세션 1000 유지 검증 |
| `myfave.chat.messages.published` | Counter | `room` | `ChatMessageController.java` (`redisPublisher.publish` 직후) | 발행 횟수 — k6 수신 카운트와 대조 |
| `myfave.chat.broadcast.duration` | Timer | `room` | `RedisSubscriber.java` (`convertAndSend` 전후) | broadcast fan-out latency (p95 < 50ms 목표) |
| `myfave.chat.ratelimit.rejected` | Counter | - | `ChatMessageService.java` `isRateLimited` 거절 분기 | 3초 throttle 작동 검증 |

## 코드 위치 참고 (탐색 결과)

- `backend/src/main/java/com/myfave/api/domain/chat/controller/ChatMessageController.java:36` — `@MessageMapping("/chat/{roomId}")`. 발행 시 `redisPublisher.publish(roomId, json)` 호출.
- `backend/src/main/java/com/myfave/api/domain/chat/service/RedisSubscriber.java:27` — `messagingTemplate.convertAndSend("/topic/chat/" + roomId, body)` (broadcast 지점).
- `backend/src/main/java/com/myfave/api/domain/chat/service/ChatMessageService.java:44-51` — `isRateLimited` Redis key `rate:chat:{userId}`, TTL 3초.
- WebSocket endpoint: `/ws` (SockJS off), STOMP broker prefix `/topic`, application prefix `/app`.

## 측정값 활용 (Grafana 패널 권장)

- **Active sessions** 라인 그래프 — 1000 평탄 구간 확인
- **Broadcast duration** percentile (p50/p95/p99) heatmap
- **Messages published / received** 비교 그래프 — 손실 0건 확인
- **Ratelimit rejected** 카운터 — 1초 간격 도배 시 증가 확인

## 통과 기준 (시나리오 E)

1. peak 동시 세션 1000 평탄 유지
2. broadcast.duration **p95 < 50ms**
3. 4.5분 부하 동안 JVM heap 증가폭 < 100MB
4. 1초 간격 의도적 도배 시 ratelimit.rejected 증가 확인
5. published == received (broadcast factor 고려)

상세 검증 절차는 노션 `k6 시나리오 검증 방법 (A/D/E)` 페이지 참고.
