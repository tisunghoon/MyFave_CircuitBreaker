package com.myfave.api.domain.chat.listener;

import com.myfave.api.domain.chat.repository.ChatRoomRepository;
import com.myfave.api.domain.chat.service.ChatService;
import com.myfave.api.domain.saleevent.entity.SaleEvent;
import com.myfave.api.domain.saleevent.event.SaleEventCreatedEvent;
import com.myfave.api.domain.saleevent.repository.SaleEventRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.ZonedDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomScheduleListener {

    private final TaskScheduler taskScheduler;
    private final ChatService chatService;
    private final SaleEventRepository saleEventRepository;
    private final ChatRoomRepository chatRoomRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSaleEventCreated(SaleEventCreatedEvent event) {
        schedule(event.getSaleEvent());
    }

    @PostConstruct
    public void recoverSchedules() {
        saleEventRepository.findBySaleStartAtAfter(ZonedDateTime.now()).stream()
                .filter(saleEvent -> chatRoomRepository.findBySaleEventAndIsActiveTrue(saleEvent).isEmpty())
                .forEach(this::schedule);
    }

    private void schedule(SaleEvent saleEvent) {
        ZonedDateTime openAt = saleEvent.getSaleStartAt().minusMinutes(30);
        if (!openAt.isAfter(ZonedDateTime.now())) return;

        taskScheduler.schedule(
                () -> chatService.openRoomForEvent(saleEvent.getSaleId()),
                openAt.toInstant()
        );
        log.info("채팅방 개설 예약: saleId={}, openAt={}", saleEvent.getSaleId(), openAt);
    }
}
