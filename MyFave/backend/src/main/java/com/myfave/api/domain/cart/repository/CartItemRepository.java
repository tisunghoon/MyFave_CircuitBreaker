package com.myfave.api.domain.cart.repository;

import com.myfave.api.domain.cart.entity.CartItem;
import com.myfave.api.domain.product.entity.Product;
import com.myfave.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // 사용자의 장바구니 목록
    List<CartItem> findByUser(User user);

    // 이미 담긴 상품인지 확인 (UNIQUE 제약 - 중복 추가 방지)
    boolean existsByUserAndProduct(User user, Product product);

    // 장바구니 전체 비우기
    void deleteByUser(User user);
}
