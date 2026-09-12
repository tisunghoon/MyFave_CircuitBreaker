package com.myfave.api.domain.product.repository;

import com.myfave.api.domain.product.entity.CategoryCode;
import com.myfave.api.domain.product.entity.Product;
import com.myfave.api.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 인플루언서가 등록한 상품 목록
    List<Product> findByUser(User user);

    // 판매 중인 상품만 (품절 제외)
    List<Product> findByIsSoldoutFalse();

    // 카테고리별 조회
    List<Product> findByCategoryCodeAndIsSoldoutFalse(CategoryCode categoryCode);

    // 상품 목록 조회 (Soft Delete 제외, 전체 카테고리)
    Page<Product> findByDeletedAtIsNull(Pageable pageable);

    // 상품 목록 조회 (Soft Delete 제외, 특정 카테고리)
    Page<Product> findByCategoryCodeAndDeletedAtIsNull(CategoryCode categoryCode, Pageable pageable);

    // 상품 상세 조회 (Soft Delete 제외)
    Optional<Product> findByProductIdAndDeletedAtIsNull(Long productId);

    // 재고 차감용 - PESSIMISTIC_WRITE 락. 매진 경쟁(시나리오 D) over-selling 방지.
    // 데드락 회피를 위해 product_id ASC 정렬 후 사용(여러 상품 동시 처리 시).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.productId = :productId and p.deletedAt is null")
    Optional<Product> findByIdForUpdate(@Param("productId") Long productId);

    // myfave.stock.snapshot: 부하테스트 시드 상품(reset.sql 기준 productId 1~10) 재고 합계 Gauge용
    @Query("select coalesce(sum(p.stockQuantity), 0) from Product p where p.productId in :productIds")
    long sumStockQuantityByProductIds(@Param("productIds") List<Long> productIds);
}
