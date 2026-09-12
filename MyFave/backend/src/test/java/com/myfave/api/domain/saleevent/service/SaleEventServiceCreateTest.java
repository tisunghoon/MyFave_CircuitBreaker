package com.myfave.api.domain.saleevent.service;

import com.myfave.api.domain.saleevent.dto.request.SaleEventCreateRequest;
import com.myfave.api.domain.saleevent.event.SaleEventCreatedEvent;
import com.myfave.api.domain.saleevent.repository.SaleEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SaleEventServiceCreateTest {

    @Mock private SaleEventRepository saleEventRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private SaleEventService saleEventService;

    private static final Long INFLUENCER_ID = 1L;

    @BeforeEach
    void setUp() {
        saleEventService = new SaleEventService(saleEventRepository, eventPublisher);
        ReflectionTestUtils.setField(saleEventService, "influencerUserId", INFLUENCER_ID);
    }

    private SaleEventCreateRequest buildRequest(String name, ZonedDateTime start, ZonedDateTime end) {
        SaleEventCreateRequest request = new SaleEventCreateRequest();
        ReflectionTestUtils.setField(request, "eventName", name);
        ReflectionTestUtils.setField(request, "saleStartAt", start);
        ReflectionTestUtils.setField(request, "saleEndAt", end);
        return request;
    }

    @Test
    @DisplayName("이벤트 생성 시 SaleEventCreatedEvent를 발행한다")
    void createEvent_publishesEvent() {
        ZonedDateTime start = ZonedDateTime.now().plusHours(2);
        ZonedDateTime end = ZonedDateTime.now().plusHours(4);
        SaleEventCreateRequest request = buildRequest("봄 세일", start, end);
        given(saleEventRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        saleEventService.createEvent(INFLUENCER_ID, request);

        then(eventPublisher).should().publishEvent(any(SaleEventCreatedEvent.class));
    }

    @Test
    @DisplayName("이벤트 생성 실패(종료 < 시작) 시 publishEvent를 호출하지 않는다")
    void createEvent_invalidTime_doesNotPublish() {
        ZonedDateTime start = ZonedDateTime.now().plusHours(4);
        ZonedDateTime end = ZonedDateTime.now().plusHours(2);
        SaleEventCreateRequest request = buildRequest("봄 세일", start, end);

        assertThatThrownBy(
                () -> saleEventService.createEvent(INFLUENCER_ID, request))
                .isInstanceOf(com.myfave.api.global.error.CustomException.class);

        then(eventPublisher).shouldHaveNoInteractions();
    }
}
