package com.myfave.api.domain.saleevent.event;

import com.myfave.api.domain.saleevent.entity.SaleEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SaleEventCreatedEvent {
    private final SaleEvent saleEvent;
}
