package com.myfave.api.domain.chat.dto.request;

import com.myfave.api.domain.chat.dto.ChatMessageType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatMessageRequest {

    @NotNull
    private ChatMessageType type;

    @NotNull
    @Valid
    private Payload payload;

    @Getter
    @NoArgsConstructor
    public static class Payload {

        @NotBlank(message = "메시지 내용을 입력해주세요.")
        @Size(max = 500, message = "메시지는 500자를 초과할 수 없습니다.")
        private String content;
    }
}
