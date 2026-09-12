package com.myfave.api.domain.coupon.controller;

import com.myfave.api.domain.coupon.dto.request.CouponIssueRequest;
import com.myfave.api.domain.coupon.dto.response.CouponIssueResponse;
import com.myfave.api.domain.coupon.dto.response.CouponResponse;
import com.myfave.api.domain.coupon.entity.CouponStatus;
import com.myfave.api.domain.coupon.service.CouponService;
import com.myfave.api.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // 8-1. 보유 쿠폰 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getMyCoupons(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) CouponStatus status) {
        List<CouponResponse> response = couponService.getMyCoupons(userId, status);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // 8-2. 사용자 쿠폰 지급 (인플루언서 전용)
    @PostMapping
    public ResponseEntity<ApiResponse<CouponIssueResponse>> issueCoupon(
            @AuthenticationPrincipal Long requesterId,
            @RequestBody @Valid CouponIssueRequest request) {
        CouponIssueResponse response = couponService.issueCoupon(requesterId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("쿠폰이 지급되었습니다.", response));
    }
}
