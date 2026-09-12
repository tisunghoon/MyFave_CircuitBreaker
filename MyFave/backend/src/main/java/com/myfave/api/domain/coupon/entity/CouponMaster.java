package com.myfave.api.domain.coupon.entity;

import com.myfave.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "coupon_masters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_master_id")
    private Long couponMasterId;

    @Column(nullable = false, length = 100)
    private String couponName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponType couponType;

    @Column(nullable = false)
    private Integer discountPrice;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Builder
    private CouponMaster(String couponName, CouponType couponType, Integer discountPrice) {
        this.couponName = couponName;
        this.couponType = couponType;
        this.discountPrice = discountPrice;
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
