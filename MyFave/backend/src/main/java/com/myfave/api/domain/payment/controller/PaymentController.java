package com.myfave.api.domain.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myfave.api.domain.payment.dto.request.PaymentCancelRequest;
import com.myfave.api.domain.payment.dto.request.PaymentConfirmRequest;
import com.myfave.api.domain.payment.dto.request.PaymentPrepareRequest;
import com.myfave.api.domain.payment.dto.request.PaymentWebhookRequest;
import com.myfave.api.domain.payment.dto.response.PaymentPrepareResponse;
import com.myfave.api.domain.payment.dto.response.PaymentResponse;
import com.myfave.api.domain.payment.service.PaymentService;
import com.myfave.api.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payment", description = "결제 API")
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "결제 준비",
            description = "서버에서 금액을 계산하고 Payment 레코드를 생성합니다.\n\n" +
                          "- 반환된 idempotencyKey를 PortOne SDK의 paymentId로 사용하세요.\n" +
                          "- 동일 주문에 진행 중인 결제가 있으면 409 반환")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "결제 준비 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류 또는 쿠폰 타입 불일치"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문 / 쿠폰 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 진행 중인 결제 또는 동시 요청 충돌")
    })
    @PostMapping("/prepare")
    public ResponseEntity<ApiResponse<PaymentPrepareResponse>> preparePayment(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid PaymentPrepareRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("결제 준비가 완료되었습니다.",
                        paymentService.preparePayment(userId, request)));
    }

    @Operation(summary = "결제 승인",
            description = "PortOne 결제창 완료 후 pgTransactionId를 서버로 전달하여 금액 검증 및 결제를 확정합니다.\n\n" +
                          "- 금액 불일치 시 자동 환불 후 400 반환\n" +
                          "- 결제 성공 시 Order 상태가 PAID로 변경됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "결제 승인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "금액 불일치 (자동 환불 처리됨)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "결제 정보 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "결제 상태 변경 불가")
    })
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid PaymentConfirmRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(paymentService.confirmPayment(userId, request)));
    }

    @Operation(summary = "결제 단건 조회",
            description = "본인 소유의 결제 한 건을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "타인 결제 접근"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "결제 정보 없음")
    })
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long paymentId) {

        return ResponseEntity.ok(ApiResponse.ok(paymentService.getPayment(userId, paymentId)));
    }

    @Operation(summary = "결제 취소/환불",
            description = "결제를 전액 또는 부분 취소합니다.\n\n" +
                          "- refundAmount가 null이거나 잔액 이상이면 전액 취소\n" +
                          "- 전액 취소 시 사용된 쿠폰은 복원됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "타인 결제 접근"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "결제 정보 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 취소되었거나 취소 불가 상태")
    })
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancelPayment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long paymentId,
            @RequestBody @Valid PaymentCancelRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(paymentService.cancelPayment(userId, paymentId, request)));
    }

    @Operation(summary = "PortOne 웹훅 수신",
            description = "PortOne에서 발송하는 결제 이벤트 웹훅을 처리합니다. HMAC-SHA256 서명을 검증합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "웹훅 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "웹훅 서명 검증 실패")
    })
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> processWebhook(
            @RequestHeader("webhook-id") String webhookId,
            @RequestHeader("webhook-timestamp") String timestamp,
            @RequestHeader("webhook-signature") String signature,
            @RequestBody String rawBody) throws Exception {

        PaymentWebhookRequest request = objectMapper.readValue(rawBody, PaymentWebhookRequest.class);
        paymentService.processWebhook(webhookId, timestamp, signature, rawBody, request);

        return ResponseEntity.ok(ApiResponse.ok("웹훅 처리 완료"));
    }
}
