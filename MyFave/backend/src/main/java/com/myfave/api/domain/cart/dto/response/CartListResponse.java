package com.myfave.api.domain.cart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CartListResponse {

    private List<CartItemResponse> cartItems;
    private int totalPrice;
    private int deliveryFee;
    private int totalPaymentPrice;
}