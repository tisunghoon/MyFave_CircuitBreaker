package com.myfave.api.domain.content.dto.response;

import com.myfave.api.domain.content.entity.ShortForm;
import com.myfave.api.domain.content.entity.ShortFormType;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class ShortFormResponse {

    private Long shortFormId;
    private ShortFormType displayType;
    private String videoUrl;
    private String thumbnailUrl;
    private Long productId;
    private String productName;
    private Integer price;

    public static ShortFormResponse from(ShortForm shortForm) {
        return new ShortFormResponse(
                shortForm.getShortFormId(),
                shortForm.getDisplayType(),
                shortForm.getVideoUrl(),
                shortForm.getThumbnailUrl(),
                shortForm.getProduct() != null ? shortForm.getProduct().getProductId() : null,
                shortForm.getProduct() != null ? shortForm.getProduct().getProductName() : null,
                shortForm.getProduct() != null ? shortForm.getProduct().getPrice() : null
        );
    }
}
