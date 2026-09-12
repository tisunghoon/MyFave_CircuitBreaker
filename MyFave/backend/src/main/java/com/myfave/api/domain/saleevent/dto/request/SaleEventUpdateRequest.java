package com.myfave.api.domain.saleevent.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.ZonedDateTime;

// PATCH용 — 모든 필드가 선택(nullable). 보낸 필드만 수정됨
// Bean Validation은 null 필드에 대해서는 검증을 스킵하므로 @NotNull 없이도 PATCH 의도 보존
@Getter
public class SaleEventUpdateRequest {

    @Size(max = 100, message = "이벤트 이름은 100자 이하입니다.")
    private String eventName;

    @Future(message = "이벤트 시작 시각은 현재 이후여야 합니다.")
    private ZonedDateTime saleStartAt;

    @Future(message = "이벤트 종료 시각은 현재 이후여야 합니다.")
    private ZonedDateTime saleEndAt;
}
