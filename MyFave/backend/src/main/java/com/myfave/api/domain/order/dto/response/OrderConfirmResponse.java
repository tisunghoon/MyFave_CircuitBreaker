package com.myfave.api.domain.order.dto.response;

import com.myfave.api.domain.order.entity.Order;
import com.myfave.api.domain.order.entity.OrderStatus;
import lombok.Getter;

@Getter
public class OrderConfirmResponse {

    // 구매확정된 주문 ID
    private final Long orderId;

    // 변경된 주문 상태 (PURCHASE_CONFIRMED)
    private final OrderStatus orderStatus;

    private OrderConfirmResponse(Long orderId, OrderStatus orderStatus) {
        this.orderId = orderId;
        this.orderStatus = orderStatus;
    }

    // Order 엔티티에서 필요한 필드만 추출하여 DTO 생성
    public static OrderConfirmResponse from(Order order) {
        return new OrderConfirmResponse(
                order.getOrderId(),
                order.getOrderStatus()
        );
    }
}
