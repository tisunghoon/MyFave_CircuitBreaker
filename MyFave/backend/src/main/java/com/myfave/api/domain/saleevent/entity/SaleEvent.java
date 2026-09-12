package com.myfave.api.domain.saleevent.entity;

import com.myfave.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Table(name = "sale_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SaleEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sale_id")
    private Long saleId;

    @Column(nullable = false, length = 100)
    private String eventName;

    @Column(nullable = false)
    private ZonedDateTime saleStartAt;

    @Column(nullable = false)
    private ZonedDateTime saleEndAt;

    @Builder
    private SaleEvent(String eventName, ZonedDateTime saleStartAt, ZonedDateTime saleEndAt) {
        this.eventName = eventName;
        this.saleStartAt = saleStartAt;
        this.saleEndAt = saleEndAt;
    }

    // PATCH용 수정 메서드 — 컬럼 추가/삭제 아님, 기존 필드 값만 변경하는 Java 로직이라 DB 마이그레이션 불필요
    // null이 아닌 필드만 업데이트 (PATCH는 보낸 필드만 변경)
    public void update(String eventName, ZonedDateTime saleStartAt, ZonedDateTime saleEndAt) {
        if (eventName != null) {
            this.eventName = eventName;
        }
        if (saleStartAt != null) {
            this.saleStartAt = saleStartAt;
        }
        if (saleEndAt != null) {
            this.saleEndAt = saleEndAt;
        }
    }
}
