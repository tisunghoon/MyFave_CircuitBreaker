package com.myfave.api.domain.content.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ContentRegisterRequest {

    @NotNull(message = "콘텐츠 유형은 필수입니다.")
    @Pattern(regexp = "SHORT_FORM|STYLE_FEED", message = "콘텐츠 유형은 SHORT_FORM 또는 STYLE_FEED여야 합니다.")
    private String contentType;

    @Pattern(regexp = "BANNER|PRODUCT_LIST", message = "숏폼 타입은 BANNER 또는 PRODUCT_LIST여야 합니다.")
    private String shortFormType;

    @NotNull(message = "상품 ID는 필수입니다.")
    @Positive(message = "상품 ID는 양수여야 합니다.")
    private Long productId;
}