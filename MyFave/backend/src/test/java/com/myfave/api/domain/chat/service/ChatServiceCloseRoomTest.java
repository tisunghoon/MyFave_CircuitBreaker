package com.myfave.api.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.myfave.api.domain.chat.dto.response.ChatRoomCloseResponse;
import com.myfave.api.domain.chat.entity.ChatRoom;
import com.myfave.api.domain.chat.repository.ChatRoomRepository;
import com.myfave.api.global.config.SessionRegistry;
import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;
import com.myfave.api.domain.user.repository.UserRepository;
import com.myfave.api.domain.saleevent.repository.SaleEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChatServiceCloseRoomTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private SessionRegistry sessionRegistry;

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SaleEventRepository saleEventRepository;

    private ChatService chatService;

    private static final Long INFLUENCER_ID = 1L;
    private static final Long REGULAR_USER_ID = 99L;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        chatService = new ChatService(chatRoomRepository, sessionRegistry, chatMessageService, objectMapper, userRepository, saleEventRepository);
        ReflectionTestUtils.setField(chatService, "influencerUserId", INFLUENCER_ID);
    }

    @Test
    @DisplayName("인플루언서가 활성 채팅방을 정상 종료한다")
    void closeChatRoom_success() {
        ChatRoom chatRoom = buildRoom(5L, true);
        given(chatRoomRepository.findTopByOrderByChatRoomIdDesc()).willReturn(Optional.of(chatRoom));

        ChatRoomCloseResponse response = chatService.closeChatRoom(INFLUENCER_ID);

        assertThat(response.getIsActive()).isFalse();
        assertThat(response.getClosedAt()).isNotNull();
    }

    @Test
    @DisplayName("인플루언서가 아닌 사용자가 요청하면 AUTH_FORBIDDEN 예외")
    void closeChatRoom_forbidden() {
        ChatRoom chatRoom = buildRoom(5L, true);
        given(chatRoomRepository.findTopByOrderByChatRoomIdDesc()).willReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> chatService.closeChatRoom(REGULAR_USER_ID))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_FORBIDDEN));
    }

    @Test
    @DisplayName("채팅방이 아예 없으면 CHAT_ROOM_NOT_FOUND 예외")
    void closeChatRoom_notFound() {
        given(chatRoomRepository.findTopByOrderByChatRoomIdDesc()).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.closeChatRoom(INFLUENCER_ID))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    @Test
    @DisplayName("이미 종료된 채팅방이면 CHAT_ROOM_ALREADY_CLOSED 예외")
    void closeChatRoom_alreadyClosed() {
        ChatRoom closedRoom = buildRoom(5L, false);
        given(chatRoomRepository.findTopByOrderByChatRoomIdDesc()).willReturn(Optional.of(closedRoom));

        assertThatThrownBy(() -> chatService.closeChatRoom(INFLUENCER_ID))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CHAT_ROOM_ALREADY_CLOSED));
    }

    private ChatRoom buildRoom(Long id, boolean active) {
        try {
            var constructor = ChatRoom.class.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            ChatRoom room = (ChatRoom) constructor.newInstance();
            var f1 = ChatRoom.class.getDeclaredField("chatRoomId");
            f1.setAccessible(true);
            f1.set(room, id);
            var f2 = ChatRoom.class.getDeclaredField("isActive");
            f2.setAccessible(true);
            f2.set(room, active);
            return room;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
