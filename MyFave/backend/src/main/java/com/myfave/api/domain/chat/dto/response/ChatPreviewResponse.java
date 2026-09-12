package com.myfave.api.domain.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class ChatPreviewResponse {

    private Boolean isActive;
    private int participantCount;
    private List<PreviewMessage> recentMessages;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PreviewMessage {
        private String senderNickname;
        private String content;
        private ZonedDateTime createdAt;
    }
}
