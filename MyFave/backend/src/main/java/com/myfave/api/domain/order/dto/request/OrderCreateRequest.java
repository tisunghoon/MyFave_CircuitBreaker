package com.myfave.api.domain.order.dto.request;

import com.myfave.api.domain.order.entity.OrderType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
public class OrderCreateRequest {

    // @NotNull: 이 필드가 null이면 @Valid 단계에서 400 에러 자동 반환
    // OrderType은 Enum이므로 DIRECT 또는 CART 이외의 값은 역직렬화 자체가 실패함
    @NotNull(message = "주문 유형은 필수입니다.")
    private OrderType orderType;

    // DIRECT 주문일 때만 값을 넣고, CART이면 null로 전송
    // @NotNull을 걸지 않는 이유: CART 주문 시에는 null이 정상
    private Long productId;

    // CART 주문일 때만 값을 넣고, DIRECT이면 null로 전송
    // 값은 product_id (상품 PK) 목록
    private List<Long> productIds;

    // 배송지는 어떤 주문 유형이든 반드시 필요
    @NotNull(message = "배송지는 필수입니다.")
    private Long shippingAddressId;
}
