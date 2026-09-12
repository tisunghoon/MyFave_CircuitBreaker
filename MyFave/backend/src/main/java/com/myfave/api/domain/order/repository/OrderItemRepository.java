package com.myfave.api.domain.order.repository;

import com.myfave.api.domain.order.entity.Order;
import com.myfave.api.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // 주문에 포함된 상품 목록
    List<OrderItem> findByOrder(Order order);
}
