package com.myfave.api.domain.product.controller;

import com.myfave.api.domain.product.dto.request.ProductRequest;
import com.myfave.api.domain.product.dto.request.ProductUpdateRequest;
import com.myfave.api.domain.product.dto.response.ProductListResponse;
import com.myfave.api.domain.product.dto.response.ProductResponse;
import com.myfave.api.domain.product.entity.CategoryCode;
import com.myfave.api.domain.product.service.ProductService;
import com.myfave.api.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 3-1. 상품 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<ProductListResponse>> getProducts(
            @RequestParam(required = false) CategoryCode categoryCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        ProductListResponse response = productService.getProducts(categoryCode, page, size, sort);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // 3-2. 상품 상세 조회
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse.Detail>> getProduct(
            @PathVariable Long productId) {
        ProductResponse.Detail response = productService.getProduct(productId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // 3-3. 상품 등록 (인플루언서 전용)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE) //이미지 파일 같이 받아야해서 일반 json 불가
    public ResponseEntity<ApiResponse<Map<String, Long>>> createProduct(
            @AuthenticationPrincipal Long userId,
            @Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProductRequest.class)))
            @RequestPart @Valid ProductRequest request,
            //이미지 파일 목록
            @RequestPart List<MultipartFile> images) {
        Long productId = productService.createProduct(userId, request, images);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Created", Map.of("id", productId)));
    }

    // 3-4. 상품 수정 (인플루언서 전용)
    @PatchMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Long>>> updateProduct(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long productId,
            @Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProductUpdateRequest.class)))
            @RequestPart(required = false) @Valid ProductUpdateRequest request,
            @RequestPart(required = false) List<MultipartFile> images) {
        Long updatedId = productService.updateProduct(userId, productId, request, images);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("id", updatedId)));
    }

    // 3-5. 상품 삭제 (인플루언서 전용, Soft Delete)
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long productId) {
        productService.deleteProduct(userId, productId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 3-6. 품절 처리 (인플루언서 전용)
    @PatchMapping("/{productId}/soldout")
    public ResponseEntity<ApiResponse<Void>> markAsSoldout(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long productId) {
        productService.markAsSoldout(userId, productId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
