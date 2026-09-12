package com.myfave.api.domain.content.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.myfave.api.domain.content.entity.ShortForm;
import com.myfave.api.domain.content.entity.StyleFeed;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContentRegisterResponse {

    private Long contentId;
    private String contentType;
    private String shortFormType;
    private String videoUrl;
    private String imageUrl;
    private String thumbnailUrl;
    private Long productId;
    private String productName;
    private ZonedDateTime createdAt;

    public static ContentRegisterResponse fromShortForm(ShortForm shortForm) {
        return new ContentRegisterResponse(
                shortForm.getShortFormId(),
                "SHORT_FORM",
                shortForm.getDisplayType().name(),
                shortForm.getVideoUrl(),
                null,
                shortForm.getThumbnailUrl(),
                shortForm.getProduct() != null ? shortForm.getProduct().getProductId() : null,
                shortForm.getProduct() != null ? shortForm.getProduct().getProductName() : null,
                shortForm.getCreatedAt()
        );
    }

    public static ContentRegisterResponse fromStyleFeed(StyleFeed styleFeed) {
        return new ContentRegisterResponse(
                styleFeed.getStyleFeedId(),
                "STYLE_FEED",
                null,
                null,
                styleFeed.getImageUrl(),
                null,
                styleFeed.getProduct().getProductId(),
                styleFeed.getProduct().getProductName(),
                styleFeed.getCreatedAt()
        );
    }
}