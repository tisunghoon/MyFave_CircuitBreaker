package com.myfave.api.domain.chat.dto.response;

import com.myfave.api.domain.chat.entity.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
@AllArgsConstructor
public class ChatRoomResponse {

    private Long chatRoomId;
    private Long saleId;
    private String eventName;
    private Boolean isActive;
    private ZonedDateTime createdAt;

    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return new ChatRoomResponse(
                chatRoom.getChatRoomId(),
                chatRoom.getSaleEvent().getSaleId(),
                chatRoom.getSaleEvent().getEventName(),
                chatRoom.getIsActive(),
                chatRoom.getCreatedAt()
        );
    }
}
