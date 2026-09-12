package com.myfave.api.domain.chat.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myfave.api.domain.chat.dto.response.ChatMessageResponse;
import com.myfave.api.domain.chat.service.RedisPublisher;
import com.myfave.api.global.config.SessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatEventListener {

    private final SessionRegistry sessionRegistry;
    private final RedisPublisher redisPublisher;
    private final ObjectMapper objectMapper;

    // sessionId → roomId 역매핑 (퇴장 시 roomId 추적용)
    private final Map<String, Long> sessionRoomMap = new ConcurrentHashMap<>();

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) throws JsonProcessingException {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith("/topic/chat/")) return;

        String sessionId = accessor.getSessionId();
        Long roomId = extractRoomId(destination);
        if (roomId == null) return;

        sessionRegistry.join(roomId, sessionId);
        sessionRoomMap.put(sessionId, roomId);

        broadcastParticipantCount(roomId);
        log.debug("입장: sessionId={}, roomId={}", sessionId, roomId);
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) throws JsonProcessingException {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Long roomId = sessionRoomMap.remove(sessionId);
        if (roomId == null) return;

        sessionRegistry.leave(roomId, sessionId);
        broadcastParticipantCount(roomId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) throws JsonProcessingException {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Long roomId = sessionRoomMap.remove(sessionId);
        if (roomId == null) return;

        sessionRegistry.leave(roomId, sessionId);
        broadcastParticipantCount(roomId);
        log.debug("퇴장: sessionId={}, roomId={}", sessionId, roomId);
    }

    private void broadcastParticipantCount(Long roomId) throws JsonProcessingException {
        int count = sessionRegistry.getParticipantCount(roomId);
        String json = objectMapper.writeValueAsString(ChatMessageResponse.participantCount(count));
        redisPublisher.publish(roomId, json);
    }

    private Long extractRoomId(String destination) {
        // "/topic/chat/{roomId}"
        try {
            String[] parts = destination.split("/");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
