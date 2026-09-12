package com.myfave.api.domain.order.dto.response;

import com.myfave.api.domain.order.entity.OrderItem;
import lombok.Getter;

// 주문 목록 조회(5-2)에서 각 주문 안의 상품 1개를 표현하는 DTO
@Getter
public class OrderItemSummaryResponse {

    private final Long productId;
    private final String productName;
    private final Integer price;
    // TODO: ProductImage 도메인 완성 후 실제 썸네일 URL로 교체
    private final String thumbnailUrl;

    private OrderItemSummaryResponse(Long productId, String productName, Integer price, String thumbnailUrl) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.thumbnailUrl = thumbnailUrl;
    }

    // OrderItem 엔티티 → DTO 변환
    // productName, price는 주문 당시 스냅샷 값 사용 (현재 상품 정보가 바뀌어도 주문 시점 값 유지)
    public static OrderItemSummaryResponse from(OrderItem orderItem) {
        return new OrderItemSummaryResponse(
                orderItem.getProduct().getProductId(),
                orderItem.getProductName(),   // 스냅샷
                orderItem.getPrice(),         // 스냅샷
                null                          // TODO: ProductImage 연동 전까지 null
        );
    }
}
