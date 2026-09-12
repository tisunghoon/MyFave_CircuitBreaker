package com.myfave.api.domain.coupon.service;

import com.myfave.api.domain.coupon.dto.request.CouponIssueRequest;
import com.myfave.api.domain.coupon.dto.response.CouponIssueResponse;
import com.myfave.api.domain.coupon.dto.response.CouponResponse;
import com.myfave.api.domain.coupon.entity.Coupon;
import com.myfave.api.domain.coupon.entity.CouponMaster;
import com.myfave.api.domain.coupon.entity.CouponStatus;
import com.myfave.api.domain.coupon.repository.CouponMasterRepository;
import com.myfave.api.domain.coupon.repository.CouponRepository;
import com.myfave.api.domain.user.entity.User;
import com.myfave.api.domain.user.repository.UserRepository;
import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private static final int DEFAULT_VALID_DAYS = 7;

    private final CouponRepository couponRepository;
    private final CouponMasterRepository couponMasterRepository;
    private final UserRepository userRepository;

    @Value("${influencer.user-id}")
    private Long influencerUserId;

    // 8-1. 보유 쿠폰 목록 조회
    @Transactional
    public List<CouponResponse> getMyCoupons(Long userId, CouponStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Lazy 만료 전환: 조회 전에 만료된 AVAILABLE 쿠폰을 EXPIRED로 일괄 처리
        couponRepository.expireAvailableCouponsBefore(user, ZonedDateTime.now());

        List<Coupon> coupons = (status == null)
                ? couponRepository.findByUser(user)
                : couponRepository.findByUserAndStatus(user, status);

        return coupons.stream()
                .map(CouponResponse::from)
                .toList();
    }

    // 8-2. 사용자 쿠폰 지급 (인플루언서 전용)
    @Transactional
    public CouponIssueResponse issueCoupon(Long requesterId, CouponIssueRequest request) {
        // 1) 인플루언서 권한 체크
        if (!influencerUserId.equals(requesterId)) {
            throw new CustomException(ErrorCode.AUTH_FORBIDDEN);
        }

        // 2) 마스터 쿠폰 조회 + 활성화 여부 검증
        CouponMaster couponMaster = couponMasterRepository.findById(request.getMasterCouponId())
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_MASTER_NOT_FOUND));

        if (Boolean.FALSE.equals(couponMaster.getIsActive())) {
            throw new CustomException(ErrorCode.COUPON_MASTER_INACTIVE);
        }

        // 3) 지급 대상 사용자 조회
        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 4) 쿠폰 발급 (발급일 + 7일 만료)
        Coupon coupon = Coupon.builder()
                .couponMaster(couponMaster)
                .user(targetUser)
                .expiredAt(ZonedDateTime.now().plusDays(DEFAULT_VALID_DAYS))
                .build();

        couponRepository.save(coupon);

        return CouponIssueResponse.from(coupon);
    }

    // 8-3. 쿠폰 사용 (Payment/Order 도메인 트랜잭션 내에서 호출)
    @Transactional
    public void useCoupon(Long couponId, Long userId) {
        Coupon coupon = couponRepository.findByIdForUpdate(couponId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

        if (!coupon.getUser().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.AUTH_FORBIDDEN);
        }

        // Lazy 만료 처리: AVAILABLE이지만 만료 지난 경우 EXPIRED 예외만 던지고 상태 전환은 다음 조회 시 벌크 쿼리가 담당
        // (여기서 expire() 호출 후 throw 하면 트랜잭션 롤백으로 변경이 손실됨)
        if (coupon.getStatus() == CouponStatus.AVAILABLE
                && coupon.getExpiredAt().isBefore(ZonedDateTime.now())) {
            throw new CustomException(ErrorCode.COUPON_EXPIRED);
        }

        switch (coupon.getStatus()) {
            case USED -> throw new CustomException(ErrorCode.COUPON_ALREADY_USED);
            case EXPIRED -> throw new CustomException(ErrorCode.COUPON_EXPIRED);
            case AVAILABLE -> coupon.use();
        }
    }

    // 8-3. 쿠폰 복구 (주문 취소/환불 트랜잭션 내에서 호출)
    // USED 상태가 아닌 경우 no-op (재시도 안전성)
    @Transactional
    public void restoreCoupon(Long couponId, Long userId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

        if (!coupon.getUser().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.AUTH_FORBIDDEN);
        }

        if (coupon.getStatus() != CouponStatus.USED) {
            log.warn("restoreCoupon called on non-USED coupon: couponId={}, status={}",
                    couponId, coupon.getStatus());
            return;
        }

        if (coupon.getExpiredAt().isBefore(ZonedDateTime.now())) {
            coupon.expire();
        } else {
            coupon.restore();
        }
    }
}
