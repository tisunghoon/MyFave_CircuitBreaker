package com.myfave.api.domain.coupon.service;

import com.myfave.api.domain.coupon.entity.Coupon;
import com.myfave.api.domain.coupon.entity.CouponMaster;
import com.myfave.api.domain.coupon.entity.CouponType;
import com.myfave.api.domain.coupon.repository.CouponMasterRepository;
import com.myfave.api.domain.coupon.repository.CouponRepository;
import com.myfave.api.domain.user.entity.User;
import com.myfave.api.domain.user.repository.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.ZonedDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CouponServiceConcurrencyTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponMasterRepository couponMasterRepository;

    @Autowired
    private UserRepository userRepository;

    // 테스트마다 새로 만들 데이터를 보관할 필드들
    private User testUser;
    private CouponMaster testCouponMaster;
    private Coupon testCoupon;

    @BeforeEach
    void setUp() {
        // 1. 테스트 사용자 만들기 (DataInitializer가 만든 user_id=1과 겹치지 않게)
        testUser = userRepository.save(
                User.builder()
                        .email("concurrency-test@example.com")
                        .password("password1234!")
                        .name("동시성테스트")
                        .nickname("conctest")
                        .phone("010-9999-9999")
                        .build()
        );

        // 2. 테스트 쿠폰 마스터 만들기 (어떤 종류의 쿠폰인지)
        testCouponMaster = couponMasterRepository.save(
                CouponMaster.builder()
                        .couponName("동시성 테스트 쿠폰")
                        .couponType(CouponType.DISCOUNT)
                        .discountPrice(1000)
                        .build()
        );

        // 3. 테스트 쿠폰 발급 (AVAILABLE 상태)
        testCoupon = couponRepository.save(
                Coupon.builder()
                        .couponMaster(testCouponMaster)
                        .user(testUser)
                        .expiredAt(ZonedDateTime.now().plusDays(7))
                        .build()
        );
    }

    @AfterEach
    void tearDown() {
        // 만든 데이터 삭제 (역순)
        couponRepository.deleteAll();
        couponMasterRepository.deleteAll();
        userRepository.delete(testUser);
    }

    @Test
    @DisplayName("Spring 컨텍스트가 정상적으로 로드되는지 확인")
    void contextLoads() {
        // 데이터가 제대로 준비되었는지만 확인
        assertThat(testUser).isNotNull();
        assertThat(testUser.getUserId()).isNotNull();
        assertThat(testCoupon).isNotNull();
        assertThat(testCoupon.getCouponId()).isNotNull();
    }

    @Test
    @DisplayName("동시 useCoupon 호출 시 쿠폰은 단 한 번만 사용되어야 한다")
    void useCoupon_concurrentRequests_shouldSucceedOnce() throws InterruptedException {
        // given - 두 스레드가 동시에 호출하도록 준비
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(1);              // 출발 신호용
        CountDownLatch done = new CountDownLatch(threadCount);     // 종료 대기용
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        Long couponId = testCoupon.getCouponId();
        Long userId = testUser.getUserId();

        // when - 두 스레드 동시 실행
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    ready.await();  // 출발 신호 대기
                    couponService.useCoupon(couponId, userId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.countDown();  // 출발!
        done.await();       // 모든 스레드가 끝날 때까지 대기
        executor.shutdown();

        // then - 한 번만 성공해야 함
        System.out.println("성공 횟수: " + successCount.get());
        System.out.println("실패 횟수: " + failCount.get());
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);
    }
}