package com.myfave.api.domain.shipping.repository;

import com.myfave.api.domain.order.entity.Order;
import com.myfave.api.domain.shipping.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    // 주문에 대한 배송 정보 (1:1)
    Optional<Delivery> findByOrder(Order order);
}
