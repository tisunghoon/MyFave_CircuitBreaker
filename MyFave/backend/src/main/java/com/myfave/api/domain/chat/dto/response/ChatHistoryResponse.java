package com.myfave.api.domain.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class ChatHistoryResponse {

    private List<MessageItem> messages;
    private boolean hasMore;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class MessageItem {
        private String messageId;
        private Long senderId;
        private String senderNickname;
        private boolean influencer;
        private String content;
        private ZonedDateTime createdAt;
    }
}
