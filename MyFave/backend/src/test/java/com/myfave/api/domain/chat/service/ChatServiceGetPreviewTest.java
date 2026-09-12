package com.myfave.api.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.myfave.api.domain.chat.dto.response.ChatPreviewResponse;
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
class ChatServiceGetPreviewTest {

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
    @DisplayName("활성 채팅방이 존재할 때 채팅방 미리보기 정보를 성공적으로 반환한다")
    void getChatPreview_success() {
        // given
        ChatRoom chatRoom = buildRoom(7L, true);
        given(chatRoomRepository.findByIsActiveTrue()).willReturn(Optional.of(chatRoom));
        given(sessionRegistry.getParticipantCount(7L)).willReturn(128);

        String msg1 = "{\"type\":\"NEW_MESSAGE\",\"payload\":{\"messageId\":\"m1\",\"userId\":42,\"nickname\":\"유저42\",\"content\":\"첫번째 메시지\",\"sentAt\":\"2026-03-22T12:05:30Z\"}}";
        String msg2 = "{\"type\":\"NEW_MESSAGE\",\"payload\":{\"messageId\":\"m2\",\"userId\":1,\"nickname\":\"MyFave_Official\",\"content\":\"두번째\",\"sentAt\":\"2026-03-22T12:06:00Z\"}}";
        given(chatMessageService.getHistory(7L)).willReturn(List.of(msg1, msg2));

        // when
        ChatPreviewResponse response = chatService.getChatPreview(5);

        // then
        assertThat(response.getIsActive()).isTrue();
        assertThat(response.getParticipantCount()).isEqualTo(128);
        assertThat(response.getRecentMessages()).hasSize(2);
        assertThat(response.getRecentMessages().get(0).getSenderNickname()).isEqualTo("유저42");
        assertThat(response.getRecentMessages().get(0).getContent()).isEqualTo("첫번째 메시지");
    }

    @Test
    @DisplayName("요청된 size보다 많은 메시지가 있으면 최근 size개만 반환한다")
    void getChatPreview_limitsToSize() {
        // given
        ChatRoom chatRoom = buildRoom(7L, true);
        given(chatRoomRepository.findByIsActiveTrue()).willReturn(Optional.of(chatRoom));
        given(sessionRegistry.getParticipantCount(7L)).willReturn(10);

        // 3개의 메시지
        String msg1 = "{\"type\":\"NEW_MESSAGE\",\"payload\":{\"messageId\":\"m1\",\"userId\":5,\"nickname\":\"A\",\"content\":\"1\",\"sentAt\":\"2026-03-22T11:00:00Z\"}}";
        String msg2 = "{\"type\":\"NEW_MESSAGE\",\"payload\":{\"messageId\":\"m2\",\"userId\":5,\"nickname\":\"A\",\"content\":\"2\",\"sentAt\":\"2026-03-22T11:01:00Z\"}}";
        String msg3 = "{\"type\":\"NEW_MESSAGE\",\"payload\":{\"messageId\":\"m3\",\"userId\":5,\"nickname\":\"A\",\"content\":\"3\",\"sentAt\":\"2026-03-22T11:02:00Z\"}}";
        given(chatMessageService.getHistory(7L)).willReturn(List.of(msg1, msg2, msg3));

        // when
        ChatPreviewResponse response = chatService.getChatPreview(2);

        // then
        assertThat(response.getRecentMessages()).hasSize(2);
        // 최근 2개 (msg2, msg3)
        assertThat(response.getRecentMessages().get(0).getContent()).isEqualTo("2");
        assertThat(response.getRecentMessages().get(1).getContent()).isEqualTo("3");
    }

    @Test
    @DisplayName("messageId와 senderId등은 제외하고 응답한다(PreviewMessage DTO 구조 검증)")
    void getChatPreview_responseFieldsAreNarrowed() {
        // given
        ChatRoom chatRoom = buildRoom(7L, true);
        given(chatRoomRepository.findByIsActiveTrue()).willReturn(Optional.of(chatRoom));
        given(sessionRegistry.getParticipantCount(7L)).willReturn(1);

        String msg = "{\"type\":\"NEW_MESSAGE\",\"payload\":{\"messageId\":\"m1\",\"userId\":1,\"nickname\":\"MyFave\",\"content\":\"hello\",\"sentAt\":\"2026-03-22T12:00:00Z\"}}";
        given(chatMessageService.getHistory(7L)).willReturn(List.of(msg));

        // when
        ChatPreviewResponse response = chatService.getChatPreview(5);

        // then
        ChatPreviewResponse.PreviewMessage pm = response.getRecentMessages().get(0);
        assertThat(pm.getSenderNickname()).isEqualTo("MyFave");
        assertThat(pm.getContent()).isEqualTo("hello");
        assertThat(pm.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("활성 채팅방이 없으면 예외를 발생시킨다")
    void getChatPreview_notFound() {
        // given
        given(chatRoomRepository.findByIsActiveTrue()).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatService.getChatPreview(5))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND));
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