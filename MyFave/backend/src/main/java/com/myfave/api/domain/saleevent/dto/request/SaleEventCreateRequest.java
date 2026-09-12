package com.myfave.api.domain.saleevent.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.ZonedDateTime;
//이벤트 등록 api 추가하면서 추가
@Getter
public class SaleEventCreateRequest {

    @NotBlank(message = "이벤트 이름은 필수입니다.")
    @Size(max = 100, message = "이벤트 이름은 100자 이하입니다.")
    private String eventName;

    @NotNull(message = "이벤트 시작 시각은 필수입니다.")
    @Future(message = "이벤트 시작 시각은 현재 이후여야 합니다.")
    private ZonedDateTime saleStartAt;

    @NotNull(message = "이벤트 종료 시각은 필수입니다.")
    @Future(message = "이벤트 종료 시각은 현재 이후여야 합니다.")
    private ZonedDateTime saleEndAt;
}
