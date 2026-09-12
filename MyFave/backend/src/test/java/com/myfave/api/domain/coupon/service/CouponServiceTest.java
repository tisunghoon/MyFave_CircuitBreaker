package com.myfave.api.domain.coupon.service;

import com.myfave.api.domain.coupon.entity.Coupon;
import com.myfave.api.domain.coupon.entity.CouponStatus;
import com.myfave.api.domain.coupon.repository.CouponMasterRepository;
import com.myfave.api.domain.coupon.repository.CouponRepository;
import com.myfave.api.domain.user.entity.User;
import com.myfave.api.domain.user.repository.UserRepository;
import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponMasterRepository couponMasterRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CouponService couponService;

    private static final Long COUPON_ID = 100L;
    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    private Coupon mockCoupon(CouponStatus status, ZonedDateTime expiredAt, Long ownerId) {
        Coupon coupon = mock(Coupon.class);
        User user = mock(User.class);
        lenient().when(coupon.getUser()).thenReturn(user);
        lenient().when(user.getUserId()).thenReturn(ownerId);
        lenient().when(coupon.getStatus()).thenReturn(status);
        lenient().when(coupon.getExpiredAt()).thenReturn(expiredAt);
        return coupon;
    }

    // ================== useCoupon ==================

    @Test
    @DisplayName("useCoupon 성공 - AVAILABLE 쿠폰 사용 시 USED 전환")
    void useCoupon_success() {
        // given
        Coupon coupon = mockCoupon(CouponStatus.AVAILABLE, ZonedDateTime.now().plusDays(3), OWNER_ID);
        given(couponRepository.findById(COUPON_ID)).willReturn(Optional.of(coupon));

        // when
        couponService.useCoupon(COUPON_ID, OWNER_ID);

        // then
        verify(coupon).use();
    }

    @Test
    @DisplayName("useCoupon 실패 - 존재하지 않는 쿠폰이면 COUPON_NOT_FOUND")
    void useCoupon_notFound() {
        // given
        given(couponRepository.findById(COUPON_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> couponService.useCoupon(COUPON_ID, OWNER_ID))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.COUPON_NOT_FOUND));
    }

    @Test
    @DisplayName("useCoupon 실패 - 소유자가 다르면 AUTH_FORBIDDEN")
    void useCoupon_notOwner() {
        // given
        Coupon coupon = mockCoupon(CouponStatus.AVAILABLE, ZonedDateTime.now().plusDays(3), OWNER_ID);
        given(couponRepository.findById(COUPON_ID)).willReturn(Optional.of(coupon));

        // when & then
        assertThatThrownBy(() -> couponService.useCoupon(COUPON_ID, OTHER_USER_ID))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_FORBIDDEN));
        verify(coupon, never()).use();
    }

    @Test
    @DisplayName("useCoupon 실패 - 이미 사용된 쿠폰이면 COUPON_ALREADY_USED")
    void useCoupon_alreadyUsed() {
        // given
        Coupon coupon = mockCoupon(CouponStatus.USED, ZonedDateTime.now().plusDays(3), OWNER_ID);
        given(couponRepository.findById(COUPON_ID)).willReturn(Optional.of(coupon));

        // when & then
        assertThatThrownBy(() -> couponService.useCoupon(COUPON_ID, OWNER_ID))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.COUPON_ALREADY_USED));
    }

    @Test
    @DisplayName("useCoupon 실패 - AVAILABLE이지만 만료 지나면 COUPON_EXPIRED (상태 전환은 다음 조회 시 벌크 쿼리가 담당)")
    void useCoupon_lazyExpired() {
        // given
        Coupon coupon = mockCoupon(CouponStatus.AVAILABLE, ZonedDateTime.now().minusDays(1), OWNER_ID);
        given(couponRepository.findById(COUPON_ID)).willReturn(Optional.of(coupon));

        // when & then
        assertThatThrownBy(() -> couponService.useCoupon(COUPON_ID, OWNER_ID))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.COUPON_EXPIRED));
        // expire() 호출 시 트랜잭션 롤백으로 변경이 손실되므로 호출하지 않음
        verify(coupon, never()).expire();
        verify(coupon, never()).use();
    }

    @Test
    @DisplayName("useCoupon 실패 - EXPIRED 상태면 COUPON_EXPIRED")
    void useCoupon_alreadyExpired() {
        // given
        Coupon coupon = mockCoupon(CouponStatus.EXPIRED, ZonedDateTime.now().minusDays(1), OWNER_ID);
        given(couponRepository.findById(COUPON_ID)).willReturn(Optional.of(coupon));

        // when & then
        assertThatThrownBy(() -> couponService.useCoupon(COUPON_ID, OWNER_ID))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.COUPON_EXPIRED));
    }

    // ================== restoreCoupon ==================

    @Test
    @DisplayName("restoreCoupon 성공 - USED + 만료 전이면 AVAILABLE로 복귀")
    void restoreCoupon_success() {
        // given
        Coupon coupon = mockCoupon(CouponStatus.USED, ZonedDateTime.now().plusDays(3), OWNER_ID);
        given(couponRepository.findById(COUPON_ID)).willReturn(Optional.of(coupon));

        // when
        couponService.restoreCoupon(COUPON_ID, OWNER_ID);

        // then
        verify(coupon).restore();
        verify(coupon, never()).expire();
    }

    @Test
    @DisplayName("restoreCoupon 성공 - USED + 만료 지났으면 EXPIRED로 전환")
    void restoreCoupon_expiredInsteadOfRestore() {
        // given
        Coupon coupon = mockCoupon(CouponStatus.USED, ZonedDateTime.now().minusDays(1), OWNER_ID);
        given(couponRepository.findById(COUPON_ID)).willReturn(Optional.of(coupon));

        // when
        couponService.restoreCoupon(COUPON_ID, OWNER_ID);

        // then
        verify(coupon).expire();
        verify(coupon, never()).restore();
    }

    @Test
    @DisplayName("restoreCoupon no-op - USED가 아닌 상태면 아무 동작 안 함 (재시도 안전성)")
    void restoreCoupon_noOpForNonUsed() {
        // given
        Coupon coupon = mockCoupon(CouponStatus.AVAILABLE, ZonedDateTime.now().plusDays(3), OWNER_ID);
        given(couponRepository.findById(COUPON_ID)).willReturn(Optional.of(coupon));

        // when
        couponService.restoreCoupon(COUPON_ID, OWNER_ID);

        // then
        verify(coupon, never()).restore();
        verify(coupon, never()).expire();
    }

    @Test
    @DisplayName("restoreCoupon 실패 - 소유자가 다르면 AUTH_FORBIDDEN")
    void restoreCoupon_notOwner() {
        // given
        Coupon coupon = mockCoupon(CouponStatus.USED, ZonedDateTime.now().plusDays(3), OWNER_ID);
        given(couponRepository.findById(COUPON_ID)).willReturn(Optional.of(coupon));

        // when & then
        assertThatThrownBy(() -> couponService.restoreCoupon(COUPON_ID, OTHER_USER_ID))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_FORBIDDEN));
        verify(coupon, never()).restore();
        verify(coupon, never()).expire();
    }

    @Test
    @DisplayName("restoreCoupon 실패 - 존재하지 않는 쿠폰이면 COUPON_NOT_FOUND")
    void restoreCoupon_notFound() {
        // given
        given(couponRepository.findById(COUPON_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> couponService.restoreCoupon(COUPON_ID, OWNER_ID))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.COUPON_NOT_FOUND));
    }

    // ================== getMyCoupons ==================

    @Test
    @DisplayName("getMyCoupons - 조회 전 expireAvailableCouponsBefore 벌크 쿼리 호출")
    void getMyCoupons_triggersLazyExpiration() {
        // given
        User user = mock(User.class);
        given(userRepository.findById(OWNER_ID)).willReturn(Optional.of(user));
        given(couponRepository.findByUser(user)).willReturn(List.of());

        // when
        couponService.getMyCoupons(OWNER_ID, null);

        // then
        verify(couponRepository).expireAvailableCouponsBefore(eq(user), any(ZonedDateTime.class));
        verify(couponRepository).findByUser(user);
    }
}
