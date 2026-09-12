package com.myfave.api.domain.content.service;

import com.myfave.api.domain.content.dto.request.ContentRegisterRequest;
import com.myfave.api.domain.content.dto.response.ContentRegisterResponse;
import com.myfave.api.domain.content.dto.response.ShortFormResponse;
import com.myfave.api.domain.content.dto.response.StyleFeedResponse;
import com.myfave.api.domain.content.entity.ShortForm;
import com.myfave.api.domain.content.entity.ShortFormType;
import com.myfave.api.domain.content.entity.StyleFeed;
import com.myfave.api.domain.content.repository.ShortFormRepository;
import com.myfave.api.domain.content.repository.StyleFeedRepository;
import com.myfave.api.domain.product.entity.Product;
import com.myfave.api.domain.product.repository.ProductRepository;
import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;
import com.myfave.api.global.util.S3UploadService;

import org.springframework.beans.factory.annotation.Value;

import lombok.RequiredArgsConstructor;

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
public class ContentService {

    private final ShortFormRepository shortFormRepository;
    private final StyleFeedRepository styleFeedRepository;
    private final ProductRepository productRepository;
    private final S3UploadService s3UploadService;

    @Value("${influencer.user-id}")
    private Long influencerUserId;

    // 9-1. 숏폼 목록 조회
    public List<ShortFormResponse> getShortForms(ShortFormType type, int size) {
        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "shortFormId"));

        List<ShortFormResponse> shortForms;

        if (type != null) {
            shortForms = shortFormRepository.findByDisplayType(type, pageable).stream()
                    .map(ShortFormResponse::from)
                    .toList();
        } else {
            shortForms = shortFormRepository.findAll(pageable).getContent().stream()
                    .map(ShortFormResponse::from)
                    .toList();
        }

        return shortForms;
    }

    // 9-2. 스타일 피드 목록 조회
    public Page<StyleFeedResponse> getStyleFeeds(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "styleFeedId"));
        return styleFeedRepository.findAll(pageable)
                .map(StyleFeedResponse::from);
    }

    // 9-3. 콘텐츠 등록
    @Transactional
    public ContentRegisterResponse registerContent(
            Long userId,
            ContentRegisterRequest request,
            MultipartFile mediaFile,
            MultipartFile thumbnailFile) {

        // 0. 인플루언서 권한 검증
        if (!influencerUserId.equals(userId)) {
            throw new CustomException(ErrorCode.AUTH_FORBIDDEN);
        }

        // 1. 상품 조회 (soft-delete된 상품 제외)
        Product product = productRepository.findByProductIdAndDeletedAtIsNull(request.getProductId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        // 2. contentType에 따라 분기
        if ("SHORT_FORM".equals(request.getContentType())) {
            // SHORT_FORM: shortFormType, thumbnailFile 필수
            if (request.getShortFormType() == null || thumbnailFile == null) {
                throw new CustomException(ErrorCode.COMMON_INVALID_INPUT);
            }

            // 파일 형식 검증
            validateShortFormMediaFile(mediaFile);
            validateImageFile(thumbnailFile);

            // S3 업로드 (영상 + 썸네일)
            String videoUrl = s3UploadService.upload(mediaFile, "contents/shortforms");
            String thumbnailUrl = s3UploadService.upload(thumbnailFile, "contents/shortforms/thumbnails");

            // ShortForm Entity 생성 및 저장
            ShortForm shortForm = ShortForm.builder()
                    .product(product)
                    .displayType(ShortFormType.valueOf(request.getShortFormType()))
                    .videoUrl(videoUrl)
                    .thumbnailUrl(thumbnailUrl)
                    .build();

            shortFormRepository.save(shortForm);
            return ContentRegisterResponse.fromShortForm(shortForm);

        } else if ("STYLE_FEED".equals(request.getContentType())) {
            // 파일 형식 검증
            validateImageFile(mediaFile);

            // STYLE_FEED: 이미지만 업로드
            String imageUrl = s3UploadService.upload(mediaFile, "contents/stylefeeds");

            // StyleFeed Entity 생성 및 저장
            StyleFeed styleFeed = StyleFeed.builder()
                    .product(product)
                    .imageUrl(imageUrl)
                    .build();

            styleFeedRepository.save(styleFeed);
            return ContentRegisterResponse.fromStyleFeed(styleFeed);

        } else {
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT);
        }
    }

    // 파일 확장자 추출
    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new CustomException(ErrorCode.FILE_INVALID_TYPE);
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    // SHORT_FORM mediaFile은 mp4만 허용
    private void validateShortFormMediaFile(MultipartFile mediaFile) {
        String ext = extractExtension(mediaFile.getOriginalFilename());
        if (!"mp4".equals(ext)) {
            throw new CustomException(ErrorCode.FILE_INVALID_TYPE);
        }
    }

    // STYLE_FEED mediaFile, thumbnailFile은 jpg/jpeg/png만 허용
    private void validateImageFile(MultipartFile file) {
        String ext = extractExtension(file.getOriginalFilename());
        if (!"jpg".equals(ext) && !"jpeg".equals(ext) && !"png".equals(ext)) {
            throw new CustomException(ErrorCode.FILE_INVALID_TYPE);
        }
    }
}