package com.myfave.api.global.error;

import com.myfave.api.global.common.ApiResponse;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MeterRegistry meterRegistry;

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        countError(errorCode.name(), errorCode.getHttpStatus());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        countError("VALIDATION_INVALID", 400);
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse(ErrorCode.COMMON_INVALID_INPUT.getMessage());
        return ResponseEntity
                .status(400)
                .body(new ApiResponse<>(400, message, null));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        countError(ErrorCode.COMMON_METHOD_NOT_ALLOWED.name(), 405);
        return ResponseEntity
                .status(405)
                .body(ApiResponse.error(ErrorCode.COMMON_METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        countError("MESSAGE_NOT_READABLE", 400);
        String message = e.getCause() != null ? e.getCause().getMessage() : ErrorCode.COMMON_INVALID_INPUT.getMessage();
        return ResponseEntity
                .status(400)
                .body(new ApiResponse<>(400, message, null));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestPart(MissingServletRequestPartException e) {
        countError("MISSING_REQUEST_PART", 400);
        return ResponseEntity
                .status(400)
                .body(new ApiResponse<>(400, "필수 파일 파라미터가 누락되었습니다.", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        countError("UNHANDLED", 500);
        log.error("Unhandled exception: {}", e.getMessage(), e);
        return ResponseEntity
                .status(500)
                .body(ApiResponse.error(ErrorCode.COMMON_INTERNAL_ERROR));
    }

    // ErrorCode.name()은 enum이라 유한(<60). VALIDATION_INVALID 등 식별자도 상수.
    // 카디널리티 안전 — Prometheus 라벨로 안전하게 사용 가능.
    private void countError(String code, int httpStatus) {
        meterRegistry.counter("myfave.error.thrown",
                "code", code,
                "http_status", String.valueOf(httpStatus)).increment();
    }
}
