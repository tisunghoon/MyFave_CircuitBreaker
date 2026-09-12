package com.myfave.api.domain.saleevent.service;

import com.myfave.api.domain.saleevent.dto.request.SaleEventCreateRequest;
import com.myfave.api.domain.saleevent.dto.request.SaleEventUpdateRequest;
import com.myfave.api.domain.saleevent.dto.response.SaleEventCreateResponse;
import com.myfave.api.domain.saleevent.dto.response.SaleEventResponse;
import com.myfave.api.domain.saleevent.dto.response.SaleEventUpdateResponse;
import com.myfave.api.domain.saleevent.entity.SaleEvent;
import com.myfave.api.domain.saleevent.event.SaleEventCreatedEvent;
import com.myfave.api.domain.saleevent.repository.SaleEventRepository;
import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SaleEventService {

    private final SaleEventRepository saleEventRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${influencer.user-id}")
    private Long influencerUserId;

    public SaleEventResponse getCurrentEvent() {
        ZonedDateTime now = ZonedDateTime.now();

        Optional<SaleEvent> liveEvent = saleEventRepository.findLiveEvent(now);
        if (liveEvent.isPresent()) {
            return SaleEventResponse.of(liveEvent.get(), true);
        }

        Optional<SaleEvent> nextEvent = saleEventRepository.findFirstBySaleStartAtAfterOrderBySaleStartAtAsc(now);
        if (nextEvent.isPresent()) {
            return SaleEventResponse.of(nextEvent.get(), false);
        }

        throw new CustomException(ErrorCode.SALE_EVENT_NOT_FOUND);
    }

    @Transactional
    public SaleEventCreateResponse createEvent(Long userId, SaleEventCreateRequest request) {
        if (!influencerUserId.equals(userId)) {
            throw new CustomException(ErrorCode.AUTH_FORBIDDEN);
        }

        if (!request.getSaleEndAt().isAfter(request.getSaleStartAt())) {
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT);
        }

        if (!request.getSaleStartAt().isAfter(ZonedDateTime.now())) {
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT);
        }

        SaleEvent saleEvent = SaleEvent.builder()
                .eventName(request.getEventName())
                .saleStartAt(request.getSaleStartAt())
                .saleEndAt(request.getSaleEndAt())
                .build();

        SaleEvent saved = saleEventRepository.save(saleEvent);
        eventPublisher.publishEvent(new SaleEventCreatedEvent(saved));
        return SaleEventCreateResponse.from(saved);
    }

    @Transactional
    public SaleEventUpdateResponse updateEvent(Long userId, Long saleId, SaleEventUpdateRequest request) {
        if (!influencerUserId.equals(userId)) {
            throw new CustomException(ErrorCode.AUTH_FORBIDDEN);
        }

        SaleEvent saleEvent = saleEventRepository.findById(saleId)
                .orElseThrow(() -> new CustomException(ErrorCode.SALE_EVENT_NOT_FOUND));

        ZonedDateTime newStart = request.getSaleStartAt() != null ? request.getSaleStartAt() : saleEvent.getSaleStartAt();
        ZonedDateTime newEnd = request.getSaleEndAt() != null ? request.getSaleEndAt() : saleEvent.getSaleEndAt();

        if (!newEnd.isAfter(newStart)) {
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT);
        }

        saleEvent.update(request.getEventName(), request.getSaleStartAt(), request.getSaleEndAt());

        return SaleEventUpdateResponse.from(saleEvent);
    }
}
