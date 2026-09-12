package com.myfave.api.domain.product.dto.request;

import com.myfave.api.domain.product.entity.CategoryCode;
import com.myfave.api.domain.product.entity.ConditionCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ProductRequest {

    @NotBlank(message = "상품명은 필수입니다")
    @Size(max = 100, message = "상품명은 100자 이하입니다")
    private String productName;

    @NotNull(message = "가격은 필수입니다")
    @PositiveOrZero(message = "가격은 0 이상이어야 합니다")
    private Integer price;

    @Size(max = 2000, message = "상품 설명은 2000자 이하입니다")
    private String description;

    @Size(max = 100, message = "한줄 소개는 100자 이하입니다")
    private String shortReview;

    @Size(max = 50, message = "사이즈는 50자 이하입니다")
    private String size;
    //enum 타입, 문자열 자동 변환
    @NotNull(message = "상품 상태는 필수입니다")
    private ConditionCode condition;
    //enum 타입, 문자열 자동 변환
    @NotNull(message = "카테고리는 필수입니다")
    private CategoryCode categoryCode;
}
