package com.myfave.api.domain.saleevent.dto.response;

import com.myfave.api.domain.saleevent.entity.SaleEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
@AllArgsConstructor
public class SaleEventCreateResponse {

    private Long id;
    private String eventName;
    private ZonedDateTime saleStartAt;
    private ZonedDateTime saleEndAt;
    private ZonedDateTime createdAt;

    public static SaleEventCreateResponse from(SaleEvent saleEvent) {
        return new SaleEventCreateResponse(
                saleEvent.getSaleId(),
                saleEvent.getEventName(),
                saleEvent.getSaleStartAt(),
                saleEvent.getSaleEndAt(),
                saleEvent.getCreatedAt()
        );
    }
}
