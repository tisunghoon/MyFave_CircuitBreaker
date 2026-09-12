package com.myfave.api.domain.product.dto.request;

import com.myfave.api.domain.product.entity.CategoryCode;
import com.myfave.api.domain.product.entity.ConditionCode;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
public class ProductUpdateRequest {

    @Size(max = 100, message = "상품명은 100자 이하입니다")
    private String productName;

    @PositiveOrZero(message = "가격은 0 이상이어야 합니다")
    private Integer price;

    @Size(max = 2000, message = "상품 설명은 2000자 이하입니다")
    private String description;

    @Size(max = 100, message = "한줄 소개는 100자 이하입니다")
    private String shortReview;

    @Size(max = 50, message = "사이즈는 50자 이하입니다")
    private String size;

    private ConditionCode condition;

    private CategoryCode categoryCode;

    private List<@Positive(message = "이미지 ID는 양수여야 합니다.") Long> deleteImageIds;
}
