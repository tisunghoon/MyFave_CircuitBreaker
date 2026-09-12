package com.myfave.api.domain.saleevent.dto.response;

import com.myfave.api.domain.saleevent.entity.SaleEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
@AllArgsConstructor
public class SaleEventResponse {

    private Long id;
    private String eventName;
    private ZonedDateTime saleStartAt;
    private ZonedDateTime saleEndAt;
    private boolean isLive;
    private ZonedDateTime serverTime; //카운트다운 동기화용

    public static SaleEventResponse of(SaleEvent saleEvent, boolean isLive) {
        return new SaleEventResponse(
                saleEvent.getSaleId(),
                saleEvent.getEventName(),
                saleEvent.getSaleStartAt(),
                saleEvent.getSaleEndAt(),
                isLive,
                ZonedDateTime.now()
        );
    }
}
