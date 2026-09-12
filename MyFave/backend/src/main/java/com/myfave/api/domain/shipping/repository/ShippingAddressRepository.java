package com.myfave.api.domain.shipping.repository;

import com.myfave.api.domain.shipping.entity.ShippingAddress;
import com.myfave.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShippingAddressRepository extends JpaRepository<ShippingAddress, Long> {

    // 사용자의 배송지 목록
    List<ShippingAddress> findByUser(User user);

    // 기본 배송지
    Optional<ShippingAddress> findByUserAndIsDefaultTrue(User user);
}
