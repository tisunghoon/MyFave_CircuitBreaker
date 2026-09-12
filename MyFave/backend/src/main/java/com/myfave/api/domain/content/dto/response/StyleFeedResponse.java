package com.myfave.api.domain.content.dto.response;

import com.myfave.api.domain.content.entity.StyleFeed;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StyleFeedResponse {

    private Long styleFeedId;
    private Long productId;
    private String imageUrl;

    public static StyleFeedResponse from(StyleFeed styleFeed) {
        return new StyleFeedResponse(
                styleFeed.getStyleFeedId(),
                styleFeed.getProduct().getProductId(),
                styleFeed.getImageUrl()
        );
    }
}
