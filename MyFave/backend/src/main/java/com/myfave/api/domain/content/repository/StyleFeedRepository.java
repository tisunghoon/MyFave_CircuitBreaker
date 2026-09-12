package com.myfave.api.domain.content.repository;

import com.myfave.api.domain.content.entity.StyleFeed;
import com.myfave.api.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StyleFeedRepository extends JpaRepository<StyleFeed, Long> {

    // 상품과 연결된 스타일 피드 목록
    List<StyleFeed> findByProduct(Product product);
}
