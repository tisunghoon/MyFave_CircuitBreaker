package com.myfave.api.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.myfave.api.domain.chat.dto.response.ChatHistoryResponse;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChatServiceGetMessageHistoryTest {

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

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        chatService = new ChatService(chatRoomRepository, sessionRegistry, chatMessageService, objectMapper, userRepository, saleEventRepository);
        ReflectionTestUtils.setField(chatService, "influencerUserId", 1L);
    }

    @Test
    @DisplayName("메시지가 있을 때 역직렬화하여 isInfluencer 포함 반환")
    void getMessageHistory_success() {
        ChatRoom chatRoom = buildRoom(10L);
        given(chatRoomRepository.findByIsActiveTrue()).willReturn(Optional.of(chatRoom));

        String msg1 = "{\"type\":\"NEW_MESSAGE\",\"payload\":{\"messageId\":\"uuid-1\",\"userId\":1,\"nickname\":\"MyFave_Official\",\"content\":\"안녕하세요\",\"sentAt\":\"2026-03-22T12:00:00Z\"}}";
        String msg2 = "{\"type\":\"NEW_MESSAGE\",\"payload\":{\"messageId\":\"uuid-2\",\"userId\":42,\"nickname\":\"패션왕\",\"content\":\"좋아요\",\"sentAt\":\"2026-03-22T12:01:00Z\"}}";
        given(chatMessageService.getHistory(10L)).willReturn(List.of(msg1, msg2));

        ChatHistoryResponse response = chatService.getMessageHistory(50, null);

        assertThat(response.getMessages()).hasSize(2);
        assertThat(response.getMessages().get(0).isInfluencer()).isTrue();
        assertThat(response.getMessages().get(1).isInfluencer()).isFalse();
        assertThat(response.isHasMore()).isFalse();
    }

    @Test
    @DisplayName("size보다 메시지가 많을 때 hasMore=true, 최근 size개 반환")
    void getMessageHistory_hasMore() {
        ChatRoom chatRoom = buildRoom(10L);
        given(chatRoomRepository.findByIsActiveTrue()).willReturn(Optional.of(chatRoom));

        String msg1 = "{\"type\":\"NEW_MESSAGE\",\"payload\":{\"messageId\":\"m1\",\"userId\":5,\"nickname\":\"A\",\"content\":\"1\",\"sentAt\":\"2026-03-22T11:00:00Z\"}}";
        String msg2 = "{\"type\":\"NEW_MESSAGE\",\"payload\":{\"messageId\":\"m2\",\"userId\":5,\"nickname\":\"A\",\"content\":\"2\",\"sentAt\":\"2026-03-22T11:01:00Z\"}}";
        String msg3 = "{\"type\":\"NEW_MESSAGE\",\"payload\":{\"messageId\":\"m3\",\"userId\":5,\"nickname\":\"A\",\"content\":\"3\",\"sentAt\":\"2026-03-22T11:02:00Z\"}}";
        given(chatMessageService.getHistory(10L)).willReturn(List.of(msg1, msg2, msg3));

        ChatHistoryResponse response = chatService.getMessageHistory(2, null);

        assertThat(response.getMessages()).hasSize(2);
        assertThat(response.isHasMore()).isTrue();
        assertThat(response.getMessages().get(0).getMessageId()).isEqualTo("m2");
        assertThat(response.getMessages().get(1).getMessageId()).isEqualTo("m3");
    }

    @Test
    @DisplayName("before 커서 이전 메시지만 반환")
    void getMessageHistory_withBeforeCursor() {
        ChatRoom chatRoom = buildRoom(10L);
        given(chatRoomRepository.findByIsActiveTrue()).willReturn(Optional.of(chatRoom));

        String msg1 = "{\"type\":\"NEW_MESSAGE\",\"payload\":{\"messageId\":\"m1\",\"userId\":5,\"nickname\":\"A\",\"content\":\"1\",\"sentAt\":\"2026-03-22T11:00:00Z\"}}";
        String msg2 = "{\"type\":\"NEW_MESSAGE\",\"payload\":{\"messageId\":\"m2\",\"userId\":5,\"nickname\":\"A\",\"content\":\"2\",\"sentAt\":\"2026-03-22T12:00:00Z\"}}";
        given(chatMessageService.getHistory(10L)).willReturn(List.of(msg1, msg2));

        ChatHistoryResponse response = chatService.getMessageHistory(50, "2026-03-22T11:30:00Z");

        assertThat(response.getMessages()).hasSize(1);
        assertThat(response.getMessages().get(0).getMessageId()).isEqualTo("m1");
    }

    @Test
    @DisplayName("NEW_MESSAGE 외 타입은 히스토리에서 제외")
    void getMessageHistory_skipsNonChatMessages() {
        ChatRoom chatRoom = buildRoom(10L);
        given(chatRoomRepository.findByIsActiveTrue()).willReturn(Optional.of(chatRoom));

        String participantMsg = "{\"type\":\"PARTICIPANT_COUNT\",\"payload\":{\"count\":5}}";
        String chatMsg = "{\"type\":\"NEW_MESSAGE\",\"payload\":{\"messageId\":\"m1\",\"userId\":5,\"nickname\":\"A\",\"content\":\"hi\",\"sentAt\":\"2026-03-22T11:00:00Z\"}}";
        given(chatMessageService.getHistory(10L)).willReturn(List.of(participantMsg, chatMsg));

        ChatHistoryResponse response = chatService.getMessageHistory(50, null);

        assertThat(response.getMessages()).hasSize(1);
        assertThat(response.getMessages().get(0).getMessageId()).isEqualTo("m1");
    }

    @Test
    @DisplayName("활성 채팅방 없으면 CHAT_ROOM_NOT_FOUND 예외")
    void getMessageHistory_roomNotFound() {
        given(chatRoomRepository.findByIsActiveTrue()).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getMessageHistory(50, null))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private ChatRoom buildRoom(Long id) {
        try {
            var constructor = ChatRoom.class.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            ChatRoom room = (ChatRoom) constructor.newInstance();
            setField(room, "chatRoomId", id);
            setField(room, "isActive", true);
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
