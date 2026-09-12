package com.myfave.api.domain.order.dto.response;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

// 주문 목록 조회(5-2) 최종 응답 DTO — 페이지네이션 메타 정보 포함
@Getter
public class OrderListResponse {

    private final List<OrderSummaryResponse> content;  // 주문 목록
    private final int page;                             // 현재 페이지 번호 (0부터 시작)
    private final int size;                             // 페이지 크기
    private final long totalElements;                   // 전체 주문 수
    private final int totalPages;                       // 전체 페이지 수
    private final boolean hasNext;                      // 다음 페이지 존재 여부

    private OrderListResponse(List<OrderSummaryResponse> content, int page, int size,
                               long totalElements, int totalPages, boolean hasNext) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
    }

    // Page<OrderSummaryResponse>로부터 페이지네이션 메타 정보를 추출해 DTO 생성
    public static OrderListResponse from(Page<OrderSummaryResponse> page) {
        return new OrderListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
