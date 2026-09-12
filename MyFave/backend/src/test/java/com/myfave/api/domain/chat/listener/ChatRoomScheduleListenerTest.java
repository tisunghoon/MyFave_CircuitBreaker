package com.myfave.api.domain.chat.listener;

import com.myfave.api.domain.chat.entity.ChatRoom;
import com.myfave.api.domain.chat.repository.ChatRoomRepository;
import com.myfave.api.domain.chat.service.ChatService;
import com.myfave.api.domain.saleevent.entity.SaleEvent;
import com.myfave.api.domain.saleevent.event.SaleEventCreatedEvent;
import com.myfave.api.domain.saleevent.repository.SaleEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ChatRoomScheduleListenerTest {

    @Mock private TaskScheduler taskScheduler;
    @Mock private ChatService chatService;
    @Mock private SaleEventRepository saleEventRepository;
    @Mock private ChatRoomRepository chatRoomRepository;

    private ChatRoomScheduleListener listener;

    @BeforeEach
    void setUp() {
        listener = new ChatRoomScheduleListener(taskScheduler, chatService, saleEventRepository, chatRoomRepository);
    }

    @Test
    @DisplayName("SaleEventCreatedEvent 수신 시 saleStartAt-30분에 태스크가 등록된다")
    void onSaleEventCreated_schedulesAtCorrectTime() {
        ZonedDateTime startAt = ZonedDateTime.now().plusHours(2);
        SaleEvent saleEvent = buildSaleEvent(1L, startAt);
        SaleEventCreatedEvent event = new SaleEventCreatedEvent(saleEvent);

        listener.onSaleEventCreated(event);

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        then(taskScheduler).should().schedule(any(Runnable.class), captor.capture());
        assertThat(captor.getValue()).isEqualTo(startAt.minusMinutes(30).toInstant());
    }

    @Test
    @DisplayName("openAt이 이미 지난 경우 스케줄을 등록하지 않는다")
    void onSaleEventCreated_skipsIfOpenAtIsPast() {
        ZonedDateTime startAt = ZonedDateTime.now().plusMinutes(10);
        SaleEvent saleEvent = buildSaleEvent(1L, startAt);
        SaleEventCreatedEvent event = new SaleEventCreatedEvent(saleEvent);

        listener.onSaleEventCreated(event);

        then(taskScheduler).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("recoverSchedules()는 미래 이벤트를 TaskScheduler에 재등록한다")
    void recoverSchedules_registersAllFutureEvents() {
        ZonedDateTime startAt = ZonedDateTime.now().plusHours(3);
        SaleEvent saleEvent = buildSaleEvent(2L, startAt);
        given(saleEventRepository.findBySaleStartAtAfter(any())).willReturn(List.of(saleEvent));
        given(chatRoomRepository.findBySaleEventAndIsActiveTrue(saleEvent)).willReturn(java.util.Optional.empty());

        listener.recoverSchedules();

        then(taskScheduler).should().schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    @DisplayName("recoverSchedules()에서 미래 이벤트가 없으면 스케줄 등록하지 않는다")
    void recoverSchedules_noFutureEvents_noSchedule() {
        given(saleEventRepository.findBySaleStartAtAfter(any())).willReturn(List.of());

        listener.recoverSchedules();

        then(taskScheduler).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("recoverSchedules()에서 이미 활성 채팅방이 있는 이벤트는 스케줄 등록하지 않는다")
    void recoverSchedules_skipsIfChatRoomAlreadyExists() {
        ZonedDateTime startAt = ZonedDateTime.now().plusHours(3);
        SaleEvent saleEvent = buildSaleEvent(3L, startAt);
        ChatRoom existingRoom = mock(ChatRoom.class);
        given(saleEventRepository.findBySaleStartAtAfter(any())).willReturn(List.of(saleEvent));
        given(chatRoomRepository.findBySaleEventAndIsActiveTrue(saleEvent)).willReturn(java.util.Optional.of(existingRoom));

        listener.recoverSchedules();

        then(taskScheduler).shouldHaveNoInteractions();
    }

    private SaleEvent buildSaleEvent(Long id, ZonedDateTime startAt) {
        SaleEvent saleEvent = SaleEvent.builder()
                .eventName("테스트 이벤트")
                .saleStartAt(startAt)
                .saleEndAt(startAt.plusHours(2))
                .build();
        try {
            var field = SaleEvent.class.getDeclaredField("saleId");
            field.setAccessible(true);
            field.set(saleEvent, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return saleEvent;
    }
}
