package com.myfave.api.domain.coupon.repository;

import com.myfave.api.domain.coupon.entity.CouponMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CouponMasterRepository extends JpaRepository<CouponMaster, Long> {

    // 발급 가능한 쿠폰 마스터 목록
    List<CouponMaster> findByIsActiveTrue();
}
