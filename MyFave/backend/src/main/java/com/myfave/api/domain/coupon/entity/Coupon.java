package com.myfave.api.domain.coupon.entity;

import com.myfave.api.domain.user.entity.User;
import com.myfave.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long couponId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_master_id", nullable = false)
    private CouponMaster couponMaster;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponStatus status = CouponStatus.AVAILABLE;

    @Column(nullable = false)
    private ZonedDateTime expiredAt;

    @Builder
    private Coupon(CouponMaster couponMaster, User user, ZonedDateTime expiredAt) {
        this.couponMaster = couponMaster;
        this.user = user;
        this.expiredAt = expiredAt;
        this.status = CouponStatus.AVAILABLE;
    }

    public void use() {
        this.status = CouponStatus.USED;
    }

    public void restore() {
        this.status = CouponStatus.AVAILABLE;
    }

    public void expire() {
        this.status = CouponStatus.EXPIRED;
    }
}
