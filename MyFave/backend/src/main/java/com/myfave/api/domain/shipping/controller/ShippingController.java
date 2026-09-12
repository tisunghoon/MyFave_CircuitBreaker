package com.myfave.api.domain.shipping.controller;

import com.myfave.api.domain.shipping.service.ShippingService;
import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.myfave.api.domain.shipping.dto.request.ShippingAddressRequest;
import com.myfave.api.domain.shipping.dto.response.DefaultAddressResponse;
import com.myfave.api.domain.shipping.dto.response.ShippingAddressResponse;
import com.myfave.api.global.common.ApiResponse;
import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;

    // 7-1. 배송지 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<ShippingAddressResponse>>> getShippingAddresses(
            @AuthenticationPrincipal Long userId) {
        if (userId == null) throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        List<ShippingAddressResponse> response = shippingService.getShippingAddresses(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // 7-2. 배송지 추가
    @PostMapping
    public ResponseEntity<ApiResponse<ShippingAddressResponse>> addShippingAddress(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid ShippingAddressRequest request) {
        if (userId == null) throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        ShippingAddressResponse response = shippingService.addShippingAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("배송지가 등록되었습니다.", response));
    }

    // 7-3. 배송지 삭제
    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteShippingAddress(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long addressId) {
        if (userId == null) throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        shippingService.deleteShippingAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.ok("배송지가 삭제되었습니다."));
    }

    // 7-4. 기본 배송지 설정
    @PatchMapping("/{addressId}/default")
    public ResponseEntity<ApiResponse<DefaultAddressResponse>> setDefaultAddress(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long addressId) {
        if (userId == null) throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        DefaultAddressResponse response = shippingService.setDefaultAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}