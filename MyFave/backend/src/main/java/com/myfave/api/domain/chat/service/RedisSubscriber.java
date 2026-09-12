package com.myfave.api.domain.chat.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final MeterRegistry meterRegistry;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel()); // "chat:room:{roomId}"
        String body = new String(message.getBody());
        String[] parts = channel.split(":");
        if (parts.length < 3) return;

        try {
            Long roomId = Long.parseLong(parts[2]);

            // Broadcast latency 측정 시작
            Timer.Sample sample = Timer.start(meterRegistry);

            messagingTemplate.convertAndSend("/topic/chat/" + roomId, body);

            // Timer 기록: broadcast 소요 시간 (room 라벨로 구분)
            sample.stop(Timer.builder("myfave.chat.broadcast.duration")
                    .description("Redis pub/sub → WebSocket broadcast 소요 시간")
                    .tag("room", String.valueOf(roomId))
                    .register(meterRegistry));

            log.debug("브로드캐스트: roomId={}", roomId);
        } catch (NumberFormatException e) {
            log.warn("채널 파싱 실패: {}", channel);
        }
    }
}