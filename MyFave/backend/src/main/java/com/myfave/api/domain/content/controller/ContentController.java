package com.myfave.api.domain.content.controller;

import com.myfave.api.domain.content.service.ContentService;
import com.myfave.api.domain.content.dto.request.ContentRegisterRequest;
import com.myfave.api.domain.content.dto.response.ContentRegisterResponse;
import com.myfave.api.domain.content.dto.response.StyleFeedResponse;
import com.myfave.api.domain.content.dto.response.ShortFormResponse;
import com.myfave.api.domain.content.entity.ShortFormType;
import com.myfave.api.global.common.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/content")
@RequiredArgsConstructor
@Validated
public class ContentController {

    private final ContentService contentService;

    // 9-1. 숏폼 목록 조회
    @GetMapping("/short-forms")
    public ResponseEntity<ApiResponse<List<ShortFormResponse>>> getShortForms(
            @RequestParam(required = false) ShortFormType type,
            @Min(1) @RequestParam(defaultValue = "10") int size) {
        List<ShortFormResponse> response = contentService.getShortForms(type, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // 9-2. 스타일 피드 목록 조회
    @GetMapping("/style-feeds")
    public ResponseEntity<ApiResponse<Page<StyleFeedResponse>>> getStyleFeeds(
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Min(1) @RequestParam(defaultValue = "12") int size) {
        Page<StyleFeedResponse> response = contentService.getStyleFeeds(page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // 9-3. 콘텐츠 등록 (Influencer)
    @PostMapping
    public ResponseEntity<ApiResponse<ContentRegisterResponse>> registerContent(
            @AuthenticationPrincipal Long userId,
            @Valid @ModelAttribute ContentRegisterRequest request,
            @RequestParam("mediaFile") MultipartFile mediaFile,
            @RequestParam(value = "thumbnailFile", required = false) MultipartFile thumbnailFile) {
        ContentRegisterResponse response = contentService.registerContent(userId, request, mediaFile, thumbnailFile);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("콘텐츠가 등록되었습니다.", response));
    }
}