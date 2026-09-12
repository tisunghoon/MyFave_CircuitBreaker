package com.myfave.api.domain.order.repository;

import com.myfave.api.domain.order.entity.Order;
import com.myfave.api.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 사용자의 주문 목록 (최신순)
    List<Order> findByUserOrderByCreatedAtDesc(User user);

    // 사용자의 주문 목록 (최신순, 페이지네이션)
    // Page<Order>: content(주문 목록) + totalElements, totalPages 등 메타 정보 포함
    Page<Order> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // 주문 번호로 조회
    Optional<Order> findByOrderNumber(String orderNumber);
}
