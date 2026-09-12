package com.myfave.api.domain.chat.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private static final String CHAT_HISTORY_KEY = "chat:history:";
    private static final long HISTORY_TTL_HOURS = 24;
    private static final int MAX_MESSAGES = 200;
    private static final Duration RATE_LIMIT_TTL = Duration.ofSeconds(3);

    private final RedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry meterRegistry;

    public void save(Long roomId, String json) {
        String key = CHAT_HISTORY_KEY + roomId;
        redisTemplate.opsForList().rightPush(key, json);
        redisTemplate.opsForList().trim(key, -MAX_MESSAGES, -1);
        redisTemplate.expire(key, Duration.ofHours(HISTORY_TTL_HOURS));
        log.debug("메시지 저장: roomId={}", roomId);
    }

    public List<String> getHistory(Long roomId) {
        List<Object> raw = redisTemplate.opsForList().range(CHAT_HISTORY_KEY + roomId, 0, -1);
        if (raw == null || raw.isEmpty()) return Collections.emptyList();
        return raw.stream().map(Objects::toString).collect(Collectors.toList());
    }

    public void deleteHistory(Long roomId) {
        redisTemplate.delete(CHAT_HISTORY_KEY + roomId);
    }

    public boolean isRateLimited(Long userId) {
        String key = "rate:chat:" + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, RATE_LIMIT_TTL);
        }
        boolean limited = count != null && count > 1;
        if (limited) {
            // rate limit으로 거절된 메시지 카운트 (throttle 작동 검증)
            Counter.builder("myfave.chat.ratelimit.rejected")
                    .description("rate limit으로 거절된 채팅 메시지 누적 수")
                    .register(meterRegistry)
                    .increment();
        }
        return limited;
    }
}