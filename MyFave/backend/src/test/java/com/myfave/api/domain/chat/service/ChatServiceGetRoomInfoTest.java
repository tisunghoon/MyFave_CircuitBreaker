package com.myfave.api.domain.chat.service;

import com.myfave.api.domain.chat.dto.response.ChatRoomInfoResponse;
import com.myfave.api.domain.chat.entity.ChatRoom;
import com.myfave.api.domain.chat.repository.ChatRoomRepository;
import com.myfave.api.global.config.SessionRegistry;
import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;
import com.myfave.api.domain.user.repository.UserRepository;
import com.myfave.api.domain.saleevent.repository.SaleEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChatServiceGetRoomInfoTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private SessionRegistry sessionRegistry;

    @InjectMocks
    private ChatService chatService;

    @Test
    @DisplayName("활성화된 채팅방이 있을 때 정보와 참가자 수를 반환한다")
    void getChatRoomInfo_success() {
        // given
        ZonedDateTime expectedCreatedAt = ZonedDateTime.parse("2026-01-01T00:00:00+09:00");
        ChatRoom chatRoom = buildActiveChatRoom(1L, expectedCreatedAt);
        given(chatRoomRepository.findByIsActiveTrue()).willReturn(Optional.of(chatRoom));
        given(sessionRegistry.getParticipantCount(1L)).willReturn(42);

        // when
        ChatRoomInfoResponse response = chatService.getChatRoomInfo();

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getIsActive()).isTrue();
        assertThat(response.getParticipantCount()).isEqualTo(42);
        assertThat(response.getCreatedAt()).isEqualTo(expectedCreatedAt);
    }

    @Test
    @DisplayName("활성화된 채팅방이 없을 때 CHAT_ROOM_NOT_FOUND 예외를 던진다")
    void getChatRoomInfo_notFound() {
        // given
        given(chatRoomRepository.findByIsActiveTrue()).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatService.getChatRoomInfo())
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private ChatRoom buildActiveChatRoom(Long id, ZonedDateTime createdAt) {
        try {
            var constructor = ChatRoom.class.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            ChatRoom room = (ChatRoom) constructor.newInstance();
            setField(room, "chatRoomId", id);
            setField(room, "isActive", true);
            setField(room, "createdAt", createdAt);
            return room;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
