package com.myfave.api.domain.chat.dto.response;

import com.myfave.api.domain.chat.entity.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
@AllArgsConstructor
public class ChatRoomInfoResponse {

    private Long id;
    private Boolean isActive;
    private int participantCount;
    private ZonedDateTime createdAt;

    public static ChatRoomInfoResponse from(ChatRoom chatRoom, int participantCount) {
        return new ChatRoomInfoResponse(
                chatRoom.getChatRoomId(),
                chatRoom.getIsActive(),
                participantCount,
                chatRoom.getCreatedAt()
        );
    }
}
