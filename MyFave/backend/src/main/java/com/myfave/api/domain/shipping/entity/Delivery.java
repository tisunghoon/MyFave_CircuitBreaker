package com.myfave.api.domain.shipping.entity;

import com.myfave.api.domain.order.entity.Order;
import com.myfave.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Table(name = "deliveries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Delivery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delivery_id")
    private Long deliveryId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orders_id", nullable = false, unique = true)
    private Order order;

    @Column(nullable = false, length = 255)
    private String receiverName;

    @Column(nullable = false, length = 20)
    private String receiverPhone;

    @Column(nullable = false, length = 255)
    private String receiverAddress;

    @Column(length = 50)
    private String deliveryRequest;

    @Column(length = 50)
    private String courierName;

    @Column(length = 50)
    private String trackingNumber;

    @Column(length = 50)
    private String carrierId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus deliveryStatus = DeliveryStatus.PREPARING;

    @Column(name = "shipped_at")
    private ZonedDateTime shippedAt;

    @Column(name = "delivered_at")
    private ZonedDateTime deliveredAt;

    @Builder
    private Delivery(Order order, String receiverName, String receiverPhone,
                     String receiverAddress, String deliveryRequest) {
        this.order = order;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.receiverAddress = receiverAddress;
        this.deliveryRequest = deliveryRequest;
        this.deliveryStatus = DeliveryStatus.PREPARING;
    }

    public void ship(String courierName, String carrierId, String trackingNumber) {
        this.courierName = courierName;
        this.carrierId = carrierId;
        this.trackingNumber = trackingNumber;
        this.deliveryStatus = DeliveryStatus.SHIPPING;
        this.shippedAt = ZonedDateTime.now();
    }

    public void deliver() {
        if (this.deliveryStatus == DeliveryStatus.DELIVERED) {
            return;
        }
        this.deliveryStatus = DeliveryStatus.DELIVERED;
        this.deliveredAt = ZonedDateTime.now();
    }

    public void cancel() {
        this.deliveryStatus = DeliveryStatus.CANCELLED;
    }
}
