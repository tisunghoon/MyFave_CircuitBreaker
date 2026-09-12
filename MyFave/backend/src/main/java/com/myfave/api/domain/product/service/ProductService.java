package com.myfave.api.domain.product.service;

import com.myfave.api.domain.product.dto.request.ProductRequest;
import com.myfave.api.domain.product.dto.request.ProductUpdateRequest;
import com.myfave.api.domain.product.dto.response.ProductListResponse;
import com.myfave.api.domain.product.dto.response.ProductResponse;
import com.myfave.api.domain.product.entity.CategoryCode;
import com.myfave.api.domain.product.entity.Product;
import com.myfave.api.domain.product.entity.ProductImage;
import com.myfave.api.domain.product.repository.ProductImageRepository;
import com.myfave.api.domain.product.repository.ProductRepository;
import com.myfave.api.domain.user.entity.User;
import com.myfave.api.domain.user.repository.UserRepository;
import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;
import com.myfave.api.global.util.S3UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository; //상품 데이터
    private final ProductImageRepository productImageRepository; //이미지 데이터
    private final UserRepository userRepository;
    private final S3UploadService s3UploadService;

    //설정파일에서 읽어오는거라 인플루언서 아이디 application.yml 에서 변경 가능
    @Value("${influencer.user-id}")
    private Long influencerUserId;

    // 3-1. 상품 목록 조회
    public ProductListResponse getProducts(CategoryCode categoryCode, int page, int size, String sort) {
        Sort sorting = resolveSort(sort);
        Pageable pageable = PageRequest.of(page, size, sorting);

        Page<Product> productPage;
        //1. 카테고리가 없거나 all 이면 전체에서 꺼내기
        if (categoryCode == null || categoryCode == CategoryCode.ALL) {
            productPage = productRepository.findByDeletedAtIsNull(pageable);
        } else {
            //2. 특정 카테고리가 있으면 그 카테고리만 꺼내기
            productPage = productRepository.findByCategoryCodeAndDeletedAtIsNull(categoryCode, pageable);
        }
        // 꺼낸 상품에 각각 대표 이미지 붙이기
        Page<ProductResponse> responsePage = productPage.map(product -> {
            String thumbnailUrl = productImageRepository.findByProductAndIsMainTrue(product)
                    .map(ProductImage::getImageUrl)
                    .orElse(null);
            return ProductResponse.from(product, thumbnailUrl);
        });

        return ProductListResponse.from(responsePage);
    }

    // 3-2. 상품 상세 조회
    public ProductResponse.Detail getProduct(Long productId) {
        // ID기반으로 찾고 못찾으면 404 에러
        Product product = productRepository.findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        List<ProductImage> images = productImageRepository.findByProductOrderBySortOrderAsc(product);

        return ProductResponse.Detail.from(product, images);
    }
    // 3-3. 상품 등록 (인플루언서 전용)
    @Transactional //쓰기 가능
    public Long createProduct(Long userId, ProductRequest request, List<MultipartFile> images) {
        validateInfluencer(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Product product = Product.builder()
                .user(user)
                .productName(request.getProductName())
                .shortReview(request.getShortReview())
                .price(request.getPrice())
                .description(request.getDescription())
                .size(request.getSize())
                .conditionCode(request.getCondition())
                .categoryCode(request.getCategoryCode())
                .build();

        productRepository.save(product);

        // 이미지 저장 (첫 번째 이미지가 메인)
        for (int i = 0; i < images.size(); i++) {
            MultipartFile image = images.get(i);
            String imageUrl = s3UploadService.upload(image, "products");

            ProductImage productImage = ProductImage.builder()
                    .product(product)
                    .imageUrl(imageUrl)
                    .sortOrder(i + 1)
                    .isMain(i == 0)
                    .build();

            productImageRepository.save(productImage);
        }

        return product.getProductId();
    }

    // 3-4. 상품 수정 (인플루언서 전용)
    @Transactional
    public Long updateProduct(Long userId, Long productId, ProductUpdateRequest request,
                              List<MultipartFile> newImages) {
        validateInfluencer(userId);
        //기존 product 가져오기 & 삭제된 상품 제외
        Product product = productRepository.findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        // 상품 정보 수정 (보낸 필드만 변경)
        if (request != null) {
            product.update(
                    request.getProductName(),
                    request.getPrice(),
                    request.getDescription(),
                    request.getShortReview(),
                    request.getSize(),
                    request.getCondition(),
                    request.getCategoryCode()
            );

            // 이미지 삭제
            if (request.getDeleteImageIds() != null && !request.getDeleteImageIds().isEmpty()) {
                productImageRepository.deleteAllByProductImgIdIn(request.getDeleteImageIds());
            }
        }

        // 새 이미지 추가
        if (newImages != null && !newImages.isEmpty()) {
            //기존 이미지 정렬 순서 중 가장 큰 값 찾아서 그 다음값 배당 (순서 중복 이슈 방지)
            int currentMaxOrder = productImageRepository.findByProductOrderBySortOrderAsc(product)
                    .stream()
                    .mapToInt(ProductImage::getSortOrder)
                    .max()
                    .orElse(0);

            for (int i = 0; i < newImages.size(); i++) {
                MultipartFile image = newImages.get(i);
                String imageUrl = s3UploadService.upload(image, "products");

                ProductImage productImage = ProductImage.builder()
                        .product(product)
                        .imageUrl(imageUrl)
                        .sortOrder(currentMaxOrder + i + 1)
                        .isMain(false)
                        .build();

                productImageRepository.save(productImage);
            }
        }

        return product.getProductId();
    }

    // 3-5. 상품 삭제 (인플루언서 전용, Soft Delete)
    @Transactional
    public void deleteProduct(Long userId, Long productId) {
        validateInfluencer(userId); //인플루언서 검증

        Product product = productRepository.findByProductIdAndDeletedAtIsNull(productId)
                //값이 없으면 예외처리, 있으면 값 꺼내서 soft delete
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        product.softDelete(); //deletedAt에 현재 시각 세팅
    }

    // 3-6. 품절 처리 (인플루언서 전용)
    @Transactional
    public void markAsSoldout(Long userId, Long productId) {
        validateInfluencer(userId);

        Product product = productRepository.findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_SOLD_OUT));

        product.markAsSoldout(); //isSoldout = true로 변경
    }

    // 인플루언서 권한 검증
    private void validateInfluencer(Long userId) {
        if (!influencerUserId.equals(userId)) {
            throw new CustomException(ErrorCode.AUTH_FORBIDDEN);
        }
    }

    // 정렬 파라미터("price,asc" 등)를 JPA Sort 객체로 변환 (기본값: 최신순)
    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sort.split(",");
        if (parts.length != 2) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String field = parts[0].trim();
        String direction = parts[1].trim().toLowerCase();
        Sort.Direction dir = "asc".equals(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, field);
    }
}
