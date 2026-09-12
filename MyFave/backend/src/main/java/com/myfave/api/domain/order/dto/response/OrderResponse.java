package com.myfave.api.domain.order.dto.response;

import com.myfave.api.domain.order.entity.Order;
import com.myfave.api.domain.order.entity.OrderStatus;
import com.myfave.api.domain.order.entity.OrderType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
@AllArgsConstructor
public class OrderResponse {

    private Long orderId;
    private String orderNumber;
    private OrderType orderType;
    private OrderStatus orderStatus;
    private ZonedDateTime createdAt;

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getOrderType(),
                order.getOrderStatus(),
                order.getCreatedAt()
        );
    }
}
