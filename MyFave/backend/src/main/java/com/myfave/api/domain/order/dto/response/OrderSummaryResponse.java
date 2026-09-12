package com.myfave.api.domain.order.dto.response;

import com.myfave.api.domain.order.entity.Order;
import com.myfave.api.domain.order.entity.OrderItem;
import com.myfave.api.domain.order.entity.OrderStatus;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.List;

// 주문 목록 조회(5-2)에서 주문 1건을 표현하는 DTO
@Getter
public class OrderSummaryResponse {

    private final Long orderId;
    private final String orderNumber;
    private final OrderStatus orderStatus;

    // Payment 도메인 미완성 → OrderItem 가격 합산으로 계산
    // TODO: Payment 도메인 완성 후 실제 결제 금액으로 교체
    private final Integer totalPaymentPrice;

    private final ZonedDateTime createdAt;
    private final List<OrderItemSummaryResponse> orderItems;

    private OrderSummaryResponse(Long orderId, String orderNumber, OrderStatus orderStatus,
                                  Integer totalPaymentPrice, ZonedDateTime createdAt,
                                  List<OrderItemSummaryResponse> orderItems) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.orderStatus = orderStatus;
        this.totalPaymentPrice = totalPaymentPrice;
        this.createdAt = createdAt;
        this.orderItems = orderItems;
    }

    // Order 엔티티 + 해당 주문의 OrderItem 목록을 받아 DTO 변환
    public static OrderSummaryResponse from(Order order, List<OrderItem> orderItems) {

        // OrderItem 가격 합산 → totalPaymentPrice
        int totalPaymentPrice = orderItems.stream()
                .mapToInt(OrderItem::getPrice)
                .sum();

        // 각 OrderItem을 OrderItemSummaryResponse로 변환
        List<OrderItemSummaryResponse> itemResponses = orderItems.stream()
                .map(OrderItemSummaryResponse::from)
                .toList();

        return new OrderSummaryResponse(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getOrderStatus(),
                totalPaymentPrice,
                order.getCreatedAt(),
                itemResponses
        );
    }
}
