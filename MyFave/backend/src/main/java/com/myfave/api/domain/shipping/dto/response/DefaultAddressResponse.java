package com.myfave.api.domain.shipping.dto.response;

import com.myfave.api.domain.shipping.entity.ShippingAddress;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DefaultAddressResponse {

    private Long shippingId;
    private Boolean isDefault;

    public static DefaultAddressResponse from(ShippingAddress shippingAddress) {
        return new DefaultAddressResponse(
                shippingAddress.getShippingId(),
                shippingAddress.getIsDefault()
        );
    }
}