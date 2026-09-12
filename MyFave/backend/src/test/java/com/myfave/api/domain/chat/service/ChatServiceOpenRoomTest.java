package com.myfave.api.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.myfave.api.domain.chat.entity.ChatRoom;
import com.myfave.api.domain.chat.repository.ChatRoomRepository;
import com.myfave.api.domain.saleevent.entity.SaleEvent;
import com.myfave.api.domain.saleevent.repository.SaleEventRepository;
import com.myfave.api.domain.user.entity.User;
import com.myfave.api.domain.user.repository.UserRepository;
import com.myfave.api.global.config.SessionRegistry;
import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ChatServiceOpenRoomTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private SessionRegistry sessionRegistry;
    @Mock private ChatMessageService chatMessageService;
    @Mock private UserRepository userRepository;
    @Mock private SaleEventRepository saleEventRepository;

    private ChatService chatService;

    private static final Long INFLUENCER_ID = 1L;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        chatService = new ChatService(chatRoomRepository, sessionRegistry, chatMessageService,
                objectMapper, userRepository, saleEventRepository);
        ReflectionTestUtils.setField(chatService, "influencerUserId", INFLUENCER_ID);
    }

    @Test
    @DisplayName("saleId로 채팅방을 생성한다")
    void openRoomForEvent_success() {
        SaleEvent saleEvent = SaleEvent.builder()
                .eventName("봄 세일")
                .saleStartAt(ZonedDateTime.now().plusHours(2))
                .saleEndAt(ZonedDateTime.now().plusHours(4))
                .build();
        User influencer = mock(User.class);
        given(chatRoomRepository.findByIsActiveTrue()).willReturn(Optional.empty());
        given(userRepository.findById(INFLUENCER_ID)).willReturn(Optional.of(influencer));
        given(saleEventRepository.findById(10L)).willReturn(Optional.of(saleEvent));

        chatService.openRoomForEvent(10L);

        then(chatRoomRepository).should().save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("이미 활성 채팅방이 있으면 저장 없이 반환한다")
    void openRoomForEvent_alreadyExists_skips() {
        ChatRoom active = buildRoom(5L, true);
        given(chatRoomRepository.findByIsActiveTrue()).willReturn(Optional.of(active));

        chatService.openRoomForEvent(10L);

        then(chatRoomRepository).should(org.mockito.Mockito.never()).save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("saleId에 해당하는 이벤트가 없으면 SALE_EVENT_NOT_FOUND 예외")
    void openRoomForEvent_saleEventNotFound() {
        User influencer = mock(User.class);
        given(chatRoomRepository.findByIsActiveTrue()).willReturn(Optional.empty());
        given(userRepository.findById(INFLUENCER_ID)).willReturn(Optional.of(influencer));
        given(saleEventRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.openRoomForEvent(99L))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.SALE_EVENT_NOT_FOUND));
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
