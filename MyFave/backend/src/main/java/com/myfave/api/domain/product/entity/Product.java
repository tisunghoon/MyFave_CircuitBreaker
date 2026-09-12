package com.myfave.api.domain.product.entity;

import com.myfave.api.domain.user.entity.User;
import com.myfave.api.global.entity.BaseEntity;
import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String productName;

    @Column(length = 255)
    private String shortReview;

    @Column(nullable = false)
    private Integer price;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String size;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConditionCode conditionCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private CategoryCode categoryCode;

    @Column(nullable = false)
    private Boolean isSoldout = false;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    private ZonedDateTime deletedAt;

    @Builder
    private Product(User user, String productName, String shortReview, Integer price,
                    String description, String size, ConditionCode conditionCode, CategoryCode categoryCode,
                    Integer stockQuantity) {
        this.user = user;
        this.productName = productName;
        this.shortReview = shortReview;
        this.price = price;
        this.description = description;
        this.size = size;
        this.conditionCode = conditionCode;
        this.categoryCode = categoryCode;
        this.isSoldout = false;
        this.stockQuantity = stockQuantity != null ? stockQuantity : 0;
    }

    //patch 보낸 것만 변경
    // Entity 상태 변경 모아서 관리하려고 메서드 하나로 묶음
    public void update(String productName, Integer price, String description,
                       String shortReview, String size, ConditionCode conditionCode,
                       CategoryCode categoryCode) {
        if (productName != null) this.productName = productName;
        if (price != null) this.price = price;
        if (description != null) this.description = description;
        if (shortReview != null) this.shortReview = shortReview;
        if (size != null) this.size = size;
        if (conditionCode != null) this.conditionCode = conditionCode;
        if (categoryCode != null) this.categoryCode = categoryCode;
    }

    public void markAsSoldout() {
        this.isSoldout = true;
    }

    // 재고 차감 — 부하 테스트 시나리오 D(매진 경쟁)의 핵심 동시성 진입점.
    // PESSIMISTIC_WRITE 락으로 얻어온 엔티티에서만 호출되어야 함(ProductRepository.findByIdForUpdate).
    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new CustomException(ErrorCode.PRODUCT_STOCK_INVALID);
        }
        if (this.stockQuantity == null || this.stockQuantity == 0) {
            throw new CustomException(ErrorCode.PRODUCT_SOLD_OUT);
        }
        if (this.stockQuantity < quantity) {
            throw new CustomException(ErrorCode.PRODUCT_STOCK_INSUFFICIENT);
        }
        this.stockQuantity -= quantity;
        if (this.stockQuantity == 0) {
            this.isSoldout = true;
        }
    }

    // 재고 복구 — 주문 취소·전액 환불 등 보상 트랜잭션에서 호출.
    // 동시성 보호(비관적 락)는 이번 작업 범위에서 제외 — k6 부하 테스트 결과에 따라 별도 이슈에서 도입 검토.
    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new CustomException(ErrorCode.PRODUCT_STOCK_INVALID);
        }
        if (this.stockQuantity == null) {
            this.stockQuantity = 0;
        }
        // 오버플로우 방어 (Integer.MAX_VALUE 근접 시)
        if ((long) this.stockQuantity + quantity > Integer.MAX_VALUE) {
            throw new CustomException(ErrorCode.PRODUCT_STOCK_RESTORE_OVERFLOW);
        }
        this.stockQuantity += quantity;
        if (this.stockQuantity > 0 && Boolean.TRUE.equals(this.isSoldout)) {
            this.isSoldout = false; // 재고 복구 시 품절 플래그 동기화
        }
    }

    // 재고 검증 전용 — 상태 변경 없이 결제 직전 재검증 등 read-only 흐름에서 사용.
    public void validateStock(int quantity) {
        if (quantity <= 0) {
            throw new CustomException(ErrorCode.PRODUCT_STOCK_INVALID);
        }
        if (this.stockQuantity == null || this.stockQuantity == 0) {
            throw new CustomException(ErrorCode.PRODUCT_SOLD_OUT);
        }
        if (this.stockQuantity < quantity) {
            throw new CustomException(ErrorCode.PRODUCT_STOCK_INSUFFICIENT);
        }
    }

    public void softDelete() {
        this.deletedAt = ZonedDateTime.now();
    }
    //order 도메인에서 삭제 여부 확인 처리하는 용도 (OrderService에 삭제된 상품 주문 불가 검증)
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
