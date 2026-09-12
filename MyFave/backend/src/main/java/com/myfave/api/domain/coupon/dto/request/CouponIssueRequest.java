package com.myfave.api.domain.coupon.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CouponIssueRequest {

    @NotNull(message = "마스터 쿠폰 ID는 필수입니다.")
    @Positive(message = "마스터 쿠폰 ID는 양수여야 합니다.")
    private Long masterCouponId;

    @NotNull(message = "지급 대상 사용자 ID는 필수입니다.")
    @Positive(message = "지급 대상 사용자 ID는 양수여야 합니다.")
    private Long userId;
}
