package com.myfave.api.domain.product.repository;

import com.myfave.api.domain.product.entity.Product;
import com.myfave.api.domain.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    // 상품의 이미지 목록 (정렬 순서대로)
    List<ProductImage> findByProductOrderBySortOrderAsc(Product product);

    // 대표 이미지
    Optional<ProductImage> findByProductAndIsMainTrue(Product product);

    // 특정 이미지 ID 목록으로 삭제
    void deleteAllByProductImgIdIn(List<Long> productImgIds);
}
