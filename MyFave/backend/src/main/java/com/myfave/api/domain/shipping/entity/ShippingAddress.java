package com.myfave.api.domain.shipping.entity;

import com.myfave.api.domain.user.entity.User;
import com.myfave.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shipping_addresses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShippingAddress extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipping_id")
    private Long shippingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String receiverName;

    @Column(nullable = false, length = 20)
    private String receiverPhone;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(length = 100)
    private String addressDetail;

    @Column(nullable = false, length = 10)
    private String zipCode;

    @Column(length = 100)
    private String deliveryRequest;

    @Column(nullable = false)
    private Boolean isDefault = false;

    @Builder
    private ShippingAddress(User user, String receiverName, String receiverPhone,
                            String address, String addressDetail, String zipCode,
                            String deliveryRequest, Boolean isDefault) {
        this.user = user;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.address = address;
        this.addressDetail = addressDetail;
        this.zipCode = zipCode;
        this.deliveryRequest = deliveryRequest;
        this.isDefault = isDefault != null ? isDefault : false;
    }

    public void setAsDefault() {
        this.isDefault = true;
    }

    public void unsetDefault() {
        this.isDefault = false;
    }
}
