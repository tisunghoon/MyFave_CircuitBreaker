package com.myfave.api.domain.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.myfave.api.domain.chat.dto.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessageResponse {

    private ChatMessageType type;
    private Object payload;

    // NEW_MESSAGE 팩토리
    public static ChatMessageResponse newMessage(String messageId, Long userId, String nickname,
                                                  String content) {
        Payload p = new Payload(messageId, userId, nickname, content,
                ZonedDateTime.now());
        return ChatMessageResponse.builder().type(ChatMessageType.NEW_MESSAGE).payload(p).build();
    }

    // 참가자 수 팩토리
    public static ChatMessageResponse participantCount(int count) {
        return ChatMessageResponse.builder()
                .type(ChatMessageType.PARTICIPANT_COUNT)
                .payload(java.util.Map.of("count", count))
                .build();
    }

    // RATE_LIMIT 팩토리
    public static ChatMessageResponse rateLimitError() {
        return ChatMessageResponse.builder().type(ChatMessageType.RATE_LIMIT).build();
    }

    // ROOM_CLOSED 팩토리
    public static ChatMessageResponse roomClosed() {
        return ChatMessageResponse.builder().type(ChatMessageType.ROOM_CLOSED).build();
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Payload {
        private String messageId;
        private Long userId;
        private String nickname;
        private String content;
        private ZonedDateTime sentAt;
    }
}
