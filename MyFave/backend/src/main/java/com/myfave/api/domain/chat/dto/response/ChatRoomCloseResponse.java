package com.myfave.api.domain.chat.dto.response;

import com.myfave.api.domain.chat.entity.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
@AllArgsConstructor
public class ChatRoomCloseResponse {

    private Boolean isActive;
    private ZonedDateTime closedAt;

    public static ChatRoomCloseResponse from(ChatRoom chatRoom) {
        return new ChatRoomCloseResponse(chatRoom.getIsActive(), chatRoom.getClosedAt());
    }
}
