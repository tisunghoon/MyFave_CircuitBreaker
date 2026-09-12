package com.myfave.api.domain.saleevent.controller;

import com.myfave.api.domain.saleevent.dto.request.SaleEventCreateRequest;
import com.myfave.api.domain.saleevent.dto.request.SaleEventUpdateRequest;
import com.myfave.api.domain.saleevent.dto.response.SaleEventCreateResponse;
import com.myfave.api.domain.saleevent.dto.response.SaleEventResponse;
import com.myfave.api.domain.saleevent.dto.response.SaleEventUpdateResponse;
import com.myfave.api.domain.saleevent.service.SaleEventService;
import com.myfave.api.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sale-events")
@RequiredArgsConstructor
public class SaleEventController {

    private final SaleEventService saleEventService;

    @GetMapping("/current") //이벤트 조회
    public ApiResponse<SaleEventResponse> getCurrentEvent() {
        return ApiResponse.ok(saleEventService.getCurrentEvent());
    }

    @PostMapping //이벤트 등록 (인플루언서 전용)
    public ApiResponse<SaleEventCreateResponse> createEvent(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid SaleEventCreateRequest request) { //JSON → Request DTO
        return ApiResponse.created("판매 이벤트가 등록되었습니다.", saleEventService.createEvent(userId, request));
    }

    @PatchMapping("/{saleId}") //이벤트 수정 (인플루언서 전용)
    public ApiResponse<SaleEventUpdateResponse> updateEvent(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long saleId,                            // URL에서 saleId 추출
            @RequestBody @Valid SaleEventUpdateRequest request) {  // JSON → Request DTO
        return ApiResponse.ok(saleEventService.updateEvent(userId, saleId, request));
    }
}
