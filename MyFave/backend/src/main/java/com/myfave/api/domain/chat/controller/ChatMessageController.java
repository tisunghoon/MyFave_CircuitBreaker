package com.myfave.api.domain.chat.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myfave.api.domain.chat.dto.ChatMessageType;
import com.myfave.api.domain.chat.dto.request.ChatMessageRequest;
import com.myfave.api.domain.chat.dto.response.ChatMessageResponse;
import com.myfave.api.domain.chat.service.ChatMessageService;
import com.myfave.api.domain.chat.service.RedisPublisher;
import com.myfave.api.domain.user.entity.User;
import com.myfave.api.domain.user.service.UserService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.HtmlUtils;

import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final UserService userService;
    private final ChatMessageService chatMessageService;
    private final RedisPublisher redisPublisher;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @MessageMapping("/chat/{roomId}")
    public void sendMessage(@DestinationVariable Long roomId,
                            @Valid ChatMessageRequest request,
                            SimpMessageHeaderAccessor headerAccessor) throws JsonProcessingException {

        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        if (userId == null) {
            log.warn("WebSocket 메시지 처리 거부: 인증되지 않은 사용자");
            return;
        }

        if (request.getType() != ChatMessageType.SEND_MESSAGE) {
            log.warn("지원하지 않는 메시지 타입: {}", request.getType());
            return;
        }

        if (chatMessageService.isRateLimited(userId)) {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId),
                    "/queue/errors",
                    objectMapper.writeValueAsString(ChatMessageResponse.rateLimitError())
            );
            return;
        }

        User user = userService.findUserById(userId);

        // XSS 방어
        String safeContent = HtmlUtils.htmlEscape(request.getPayload().getContent());

        ChatMessageResponse response = ChatMessageResponse.newMessage(
                UUID.randomUUID().toString(),
                userId,
                user.getNickname(),
                safeContent
        );

        String json = objectMapper.writeValueAsString(response);
        chatMessageService.save(roomId, json);
        redisPublisher.publish(roomId, json);

        // 메시지 발행 카운트 (k6 수신과 대조)
        Counter.builder("myfave.chat.messages.published")
                .description("클라이언트가 발행한 채팅 메시지 누적 수")
                .tag("room", String.valueOf(roomId))
                .register(meterRegistry)
                .increment();
    }

    @MessageExceptionHandler
    public void handleException(Exception e, SimpMessageHeaderAccessor headerAccessor)
            throws JsonProcessingException {
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        if (userId == null) userId = 1L;
        log.warn("WebSocket 메시지 처리 오류: {}", e.getMessage());
        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                "/queue/errors",
                objectMapper.writeValueAsString(
                        ChatMessageResponse.builder().type(ChatMessageType.ERROR).build()
                )
        );
    }
}