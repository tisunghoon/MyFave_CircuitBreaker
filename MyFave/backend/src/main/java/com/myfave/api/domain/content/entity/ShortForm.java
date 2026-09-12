package com.myfave.api.domain.content.entity;

import com.myfave.api.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.ZonedDateTime;

@Entity
@Table(name = "short_forms")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShortForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "short_form_id")
    private Long shortFormId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;   // BANNER 타입은 상품 미연결 허용 (NULL)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShortFormType displayType;

    @Column(nullable = false, length = 500)
    private String videoUrl;

    @Column(nullable = false, length = 500)
    private String thumbnailUrl;

    @CreatedDate
    @Column(updatable = false)
    private ZonedDateTime createdAt;

    @Builder
    private ShortForm(Product product, ShortFormType displayType, String videoUrl, String thumbnailUrl) {
        this.product = product;
        this.displayType = displayType;
        this.videoUrl = videoUrl;
        this.thumbnailUrl = thumbnailUrl;
    }
}
