package com.myfave.api.domain.cart.controller;

import com.myfave.api.domain.cart.service.CartService;
import com.myfave.api.domain.cart.dto.request.CartItemRequest;
import com.myfave.api.domain.cart.dto.response.CartListResponse;
import com.myfave.api.global.common.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart-items")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // 4-1. 장바구니 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<CartListResponse>> getCartItems(
            @AuthenticationPrincipal Long userId) {
        CartListResponse response = cartService.getCartItems(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // 4-2. 장바구니에 상품 추가
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> addCartItem(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid CartItemRequest request) {
        var response = cartService.addCartItem(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("장바구니에 추가되었습니다.", response));
    }

    // 4-3. 장바구니 개별 삭제
    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> deleteCartItem(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long cartItemId) {
        cartService.deleteCartItem(userId, cartItemId);
        return ResponseEntity.ok(new ApiResponse<>(200, "장바구니에서 삭제되었습니다.", null));
    }

    // 4-4. 장바구니 전체 삭제
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteAllCartItems(
            @AuthenticationPrincipal Long userId) {
        cartService.deleteAllCartItems(userId);
        return ResponseEntity.ok(new ApiResponse<>(200, "장바구니가 비워졌습니다.", null));
    }
}