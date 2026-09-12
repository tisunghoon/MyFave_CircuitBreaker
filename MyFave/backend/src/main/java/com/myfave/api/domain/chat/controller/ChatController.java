package com.myfave.api.domain.chat.controller;

import com.myfave.api.domain.chat.dto.response.ChatHistoryResponse;
import com.myfave.api.domain.chat.dto.response.ChatPreviewResponse;
import com.myfave.api.domain.chat.dto.response.ChatRoomCloseResponse;
import com.myfave.api.domain.chat.dto.response.ChatRoomInfoResponse;
import com.myfave.api.domain.chat.service.ChatService;
import com.myfave.api.global.common.ApiResponse;
import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat-room")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    public ResponseEntity<ApiResponse<ChatRoomInfoResponse>> getChatRoom() {
        return ResponseEntity.ok(ApiResponse.ok(chatService.getChatRoomInfo()));
    }

    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<ChatHistoryResponse>> getMessages(
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String before) {

        if (size < 1 || size > 100) {
            return ResponseEntity.<ApiResponse<ChatHistoryResponse>>badRequest().body(ApiResponse.error(ErrorCode.COMMON_INVALID_INPUT));
        }

        return ResponseEntity.ok(ApiResponse.ok(chatService.getMessageHistory(size, before)));
    }

    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<ChatPreviewResponse>> getChatPreview(
            @RequestParam(defaultValue = "5") int size) {
        if (size < 1 || size > 100) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.COMMON_INVALID_INPUT));
        }
        return ResponseEntity.ok(ApiResponse.ok(chatService.getChatPreview(size)));
    }

    @PatchMapping("/close")
    public ResponseEntity<ApiResponse<ChatRoomCloseResponse>> closeChatRoom(
            Authentication authentication) {

        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(new ApiResponse<>(200, "채팅방이 종료되었습니다.", chatService.closeChatRoom(userId)));
    }
}
