package com.myfave.api.domain.shipping.dto.response;

import com.myfave.api.domain.shipping.entity.ShippingAddress;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ShippingAddressResponse {

    private Long shippingId;
    private String receiverName;
    private String receiverPhone;
    private String address;
    private String addressDetail;
    private String zipCode;
    private String deliveryRequest;
    private Boolean isDefault;

    public static ShippingAddressResponse from(ShippingAddress shippingAddress) {
        return new ShippingAddressResponse(
                shippingAddress.getShippingId(),
                shippingAddress.getReceiverName(),
                shippingAddress.getReceiverPhone(),
                shippingAddress.getAddress(),
                shippingAddress.getAddressDetail(),
                shippingAddress.getZipCode(),
                shippingAddress.getDeliveryRequest(),
                shippingAddress.getIsDefault()
        );
    }
}
