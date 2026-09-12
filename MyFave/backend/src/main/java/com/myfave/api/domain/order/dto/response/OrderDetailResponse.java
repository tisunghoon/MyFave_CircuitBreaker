package com.myfave.api.domain.order.dto.response;

import com.myfave.api.domain.order.entity.Order;
import com.myfave.api.domain.order.entity.OrderItem;
import com.myfave.api.domain.order.entity.OrderStatus;
import com.myfave.api.domain.payment.entity.Payment;
import com.myfave.api.domain.payment.entity.PaymentMethod;
import com.myfave.api.domain.shipping.entity.Delivery;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.List;

// 주문 상세 조회(5-3) 응답 DTO
// Order + Payment + Delivery + OrderItem 정보를 하나로 묶어 반환
@Getter
public class OrderDetailResponse {

    // ── Order 기본 정보 ─────────────────────────────────────────────
    private final Long orderId;
    private final String orderNumber;
    private final OrderStatus orderStatus;
    private final ZonedDateTime createdAt;
    private final ZonedDateTime updatedAt;

    // ── Payment 정보 (미결제 상태이면 null) ─────────────────────────
    private final Integer totalProductPrice;
    private final Integer deliveryFee;
    private final Integer discountPrice;
    private final Integer totalPaymentPrice;
    private final PaymentMethod paymentMethod;

    // ── Delivery 정보 (배송 생성 전이면 null) ────────────────────────
    private final String receiverName;
    private final String receiverPhone;
    private final String receiverAddress;
    private final String deliveryRequest;
    private final String trackingNumber;
    private final String courierName;

    // ── 주문 상품 목록 ───────────────────────────────────────────────
    private final List<OrderItemSummaryResponse> orderItems;

    private OrderDetailResponse(Long orderId, String orderNumber, OrderStatus orderStatus,
                                 ZonedDateTime createdAt, ZonedDateTime updatedAt,
                                 Integer totalProductPrice, Integer deliveryFee,
                                 Integer discountPrice, Integer totalPaymentPrice,
                                 PaymentMethod paymentMethod,
                                 String receiverName, String receiverPhone,
                                 String receiverAddress, String deliveryRequest,
                                 String trackingNumber, String courierName,
                                 List<OrderItemSummaryResponse> orderItems) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.orderStatus = orderStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.totalProductPrice = totalProductPrice;
        this.deliveryFee = deliveryFee;
        this.discountPrice = discountPrice;
        this.totalPaymentPrice = totalPaymentPrice;
        this.paymentMethod = paymentMethod;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.receiverAddress = receiverAddress;
        this.deliveryRequest = deliveryRequest;
        this.trackingNumber = trackingNumber;
        this.courierName = courierName;
        this.orderItems = orderItems;
    }

    // Order + Payment(nullable) + Delivery(nullable) + OrderItem 목록 → DTO 변환
    public static OrderDetailResponse from(Order order, Payment payment,
                                            Delivery delivery, List<OrderItem> orderItems) {

        // OrderItem 목록 → OrderItemSummaryResponse 목록으로 변환
        List<OrderItemSummaryResponse> itemResponses = orderItems.stream()
                .map(OrderItemSummaryResponse::from)
                .toList();

        return new OrderDetailResponse(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getOrderStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt(),

                // Payment가 null이면(미결제) 각 필드도 null 반환
                payment != null ? payment.getTotalProductPrice() : null,
                payment != null ? payment.getDeliveryFee()       : null,
                payment != null ? payment.getDiscountPrice()     : null,
                payment != null ? payment.getTotalPaymentPrice() : null,
                payment != null ? payment.getPaymentMethod()     : null,

                // Delivery가 null이면(배송 생성 전) 각 필드도 null 반환
                delivery != null ? delivery.getReceiverName()    : null,
                delivery != null ? delivery.getReceiverPhone()   : null,
                delivery != null ? delivery.getReceiverAddress() : null,
                delivery != null ? delivery.getDeliveryRequest() : null,
                delivery != null ? delivery.getTrackingNumber()  : null,
                delivery != null ? delivery.getCourierName()     : null,

                itemResponses
        );
    }
}
