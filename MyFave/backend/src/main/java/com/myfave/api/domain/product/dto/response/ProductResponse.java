package com.myfave.api.domain.product.dto.response;

import com.myfave.api.domain.product.entity.CategoryCode;
import com.myfave.api.domain.product.entity.ConditionCode;
import com.myfave.api.domain.product.entity.Product;
import com.myfave.api.domain.product.entity.ProductImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String productName;
    private Integer price;
    private String thumbnailUrl;
    private Boolean isSoldOut;
    private CategoryCode categoryCode;

    public static ProductResponse from(Product product, String thumbnailUrl) {
        return new ProductResponse(
                product.getProductId(),
                product.getProductName(),
                product.getPrice(),
                thumbnailUrl,
                product.getIsSoldout(),
                product.getCategoryCode()
        );
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Detail {
        private Long id;
        private String productName;
        private String shortReview;
        private Integer price;
        private String description;
        private String size;
        private ConditionCode condition;
        private CategoryCode categoryCode;
        private Boolean isSoldOut;
        private List<ImageDto> images;
        private ZonedDateTime createdAt;

        public static Detail from(Product product, List<ProductImage> images) {
            List<ImageDto> imageDtos = images.stream()
                    .map(ImageDto::from)
                    .toList();

            return Detail.builder()
                    .id(product.getProductId())
                    .productName(product.getProductName())
                    .shortReview(product.getShortReview())
                    .price(product.getPrice())
                    .description(product.getDescription())
                    .size(product.getSize())
                    .condition(product.getConditionCode())
                    .categoryCode(product.getCategoryCode())
                    .isSoldOut(product.getIsSoldout())
                    .images(imageDtos)
                    .createdAt(product.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @AllArgsConstructor
    public static class ImageDto {
        private Long imageId;
        private String imageUrl;
        private Integer sortOrder;
        private Boolean isMain;

        public static ImageDto from(ProductImage image) {
            return new ImageDto(
                    image.getProductImgId(),
                    image.getImageUrl(),
                    image.getSortOrder(),
                    image.getIsMain()
            );
        }
    }
}
