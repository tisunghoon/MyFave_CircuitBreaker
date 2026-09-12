package com.myfave.api.domain.order.controller;

import com.myfave.api.domain.order.dto.request.OrderCreateRequest;
import com.myfave.api.domain.order.dto.response.OrderConfirmResponse;
import com.myfave.api.domain.order.dto.response.OrderDetailResponse;
import com.myfave.api.domain.order.dto.response.OrderListResponse;
import com.myfave.api.domain.order.dto.response.OrderResponse;
import com.myfave.api.domain.order.service.OrderService;
import com.myfave.api.domain.shipping.dto.response.TrackingResponse;
import com.myfave.api.domain.shipping.service.ShippingService;
import com.myfave.api.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// @Tag: Swagger UI에서 이 Controller의 API들을 "Order" 그룹으로 묶어서 표시
@Tag(name = "Order", description = "주문 API")
@RestController
@RequestMapping("/orders")  // context-path(/api/v1)가 자동으로 앞에 붙어 실제 URL은 /api/v1/orders
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ShippingService shippingService;

    /**
     * 주문 생성 (5-1)
     * POST /api/v1/orders
     */
    // @Operation: Swagger UI에 표시될 API 제목과 설명
    @Operation(
            summary = "주문 생성",
            description = "바로구매(DIRECT) 또는 장바구니(CART) 주문을 생성합니다.\n\n" +
                          "- DIRECT: orderType=DIRECT, productId에 상품 ID 입력, cartItemIds=null\n" +
                          "- CART: orderType=CART, cartItemIds에 장바구니 ID 목록 입력, productId=null"
    )
    // @ApiResponses: Swagger UI에 각 HTTP 상태 코드별 설명 표시
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "주문 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류 또는 유효하지 않은 주문 유형"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 소유가 아닌 배송지 또는 장바구니 항목"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품 / 배송지 / 장바구니 항목 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "품절된 상품 포함")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            // @AuthenticationPrincipal: JwtAuthenticationFilter가 SecurityContext에 저장한
            //   principal(userId, Long 타입)을 파라미터로 자동 주입
            // SecurityConfig가 현재 permitAll이므로 토큰 없이도 호출 가능하지만,
            //   토큰이 없으면 userId가 null로 들어옴 → 서비스에서 USER_NOT_FOUND 발생
            @AuthenticationPrincipal Long userId,

            // @RequestBody: HTTP 요청 본문(JSON)을 OrderCreateRequest 객체로 변환
            // @Valid: OrderCreateRequest의 @NotNull 등 유효성 검증 실행
            //   검증 실패 시 GlobalExceptionHandler의 handleValidationException()이 400 반환
            @RequestBody @Valid OrderCreateRequest request) {

        OrderResponse response = orderService.createOrder(userId, request);

        // ApiResponse.created(): code=201, message="주문이 생성되었습니다.", data=response
        // ResponseEntity.status(CREATED): HTTP 상태 코드를 201로 설정
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("주문이 생성되었습니다.", response));
    }

    /**
     * 주문 목록 조회 (5-2)
     * GET /api/v1/orders?page=0&size=20
     */
    @Operation(
            summary = "주문 목록 조회",
            description = "로그인한 사용자의 주문 목록을 최신순으로 페이지네이션하여 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주문 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<OrderListResponse>> getOrders(
            @AuthenticationPrincipal Long userId,

            // @RequestParam: URL 쿼리 파라미터(?page=0&size=20)를 바인딩
            // defaultValue: 파라미터가 없을 때 사용할 기본값
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // PageRequest.of(page, size): 페이지 번호와 크기를 Pageable 객체로 변환
        Pageable pageable = PageRequest.of(page, size);

        OrderListResponse response = orderService.getOrders(userId, pageable);

        // ApiResponse.ok(): code=200, message="OK", data=response
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 주문 상세 조회 (5-3)
     * GET /api/v1/orders/{orderId}
     */
    @Operation(
            summary = "주문 상세 조회",
            description = "특정 주문의 상세 정보를 반환합니다.\n\n" +
                          "- 미결제 상태이면 Payment 관련 필드(totalProductPrice 등)는 null\n" +
                          "- 배송 생성 전이면 Delivery 관련 필드(receiverName 등)는 null"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주문 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 주문이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문 없음")
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderDetail(
            @AuthenticationPrincipal Long userId,

            // @PathVariable: URL 경로의 {orderId} 값을 파라미터로 바인딩
            @PathVariable Long orderId) {

        OrderDetailResponse response = orderService.getOrderDetail(userId, orderId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 주문 상태 변경 - 구매확정 (5-4)
     * PATCH /api/v1/orders/{orderId}/confirm
     */
    @Operation(
            summary = "구매확정",
            description = "배송완료(DELIVERY_COMPLETED) 상태의 주문을 구매확정(PURCHASE_CONFIRMED)으로 변경합니다.\n\n" +
                          "- DELIVERY_COMPLETED 상태가 아니면 409 반환"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "구매확정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 주문이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "구매확정 가능한 상태가 아님")
    })
    @PatchMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponse<OrderConfirmResponse>> confirmOrder(
            @AuthenticationPrincipal Long userId,

            // @PathVariable: URL 경로의 {orderId} 값을 파라미터로 바인딩
            @PathVariable Long orderId) {

        OrderConfirmResponse response = orderService.confirmOrder(userId, orderId);

        // ApiResponse.ok(): code=200, message="OK", data=response
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 배송 추적 조회 (5-5)
     * GET /api/v1/orders/{orderId}/tracking
     */
    @Operation(
            summary = "배송 추적 조회",
            description = "CU 편의점 택배 현재 배송 상태를 조회합니다.\n\n" +
                          "- DELIVERED 감지 시 주문 상태가 DELIVERY_COMPLETED로 자동 갱신됩니다.\n" +
                          "- 운송장 미등록(배송 준비 중) 상태이면 400 반환"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "운송장 미등록"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 주문 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "배송 추적 서비스 오류")
    })
    @GetMapping("/{orderId}/tracking")
    public ResponseEntity<ApiResponse<TrackingResponse>> trackDelivery(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long orderId) {
        TrackingResponse response = shippingService.trackDelivery(userId, orderId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
