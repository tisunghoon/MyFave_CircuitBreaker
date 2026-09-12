package com.myfave.api.domain.cart.dto.response;

import com.myfave.api.domain.cart.entity.CartItem;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CartItemResponse {

    private Long cartId;
    private Long productId;
    private String productName;
    private Integer price;
    private Boolean isSoldOut;

    public static CartItemResponse from(CartItem cartItem) {
        return new CartItemResponse(
                cartItem.getCartId(),
                cartItem.getProduct().getProductId(),
                cartItem.getProduct().getProductName(),
                cartItem.getProduct().getPrice(),
                cartItem.getProduct().getIsSoldout()
        );
    }
}
