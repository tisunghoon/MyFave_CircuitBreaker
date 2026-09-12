package com.myfave.api.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(Long roomId, String message) {
        String channel = "chat:room:" + roomId;
        redisTemplate.convertAndSend(channel, message);
        log.debug("Redis 발행: channel={}", channel);
    }
}
