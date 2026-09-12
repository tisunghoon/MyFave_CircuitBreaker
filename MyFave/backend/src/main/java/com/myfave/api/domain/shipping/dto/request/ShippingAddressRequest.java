package com.myfave.api.domain.shipping.dto.request;

import lombok.Getter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Getter
public class ShippingAddressRequest {

    @NotBlank(message = "수령인 이름은 필수입니다.")
    @Size(min = 2, max = 20, message = "수령인 이름은 2~20자여야 합니다.")
    private String receiverName;

    @NotBlank(message = "수령인 전화번호는 필수입니다.")
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "전화번호는 010-XXXX-XXXX 형식이어야 합니다.")
    private String receiverPhone;

    @NotBlank(message = "주소는 필수입니다.")
    private String address;

    @Size(max = 100, message = "상세주소는 100자 이내여야 합니다.")
    private String addressDetail;

    @NotBlank(message = "우편번호는 필수입니다.")
    @Pattern(regexp = "^\\d{5}$", message = "우편번호는 5자리 숫자여야 합니다.")
    private String zipCode;

    @Size(max = 100, message = "배송 요청사항은 100자 이내여야 합니다.")
    private String deliveryRequest;
}
