package com.myfave.api.domain.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myfave.api.domain.chat.dto.response.ChatHistoryResponse;
import com.myfave.api.domain.chat.dto.response.ChatPreviewResponse;
import com.myfave.api.domain.chat.dto.response.ChatRoomCloseResponse;
import com.myfave.api.domain.chat.dto.response.ChatRoomInfoResponse;
import com.myfave.api.domain.chat.entity.ChatRoom;
import com.myfave.api.domain.chat.repository.ChatRoomRepository;
import com.myfave.api.global.config.SessionRegistry;
import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;
import com.myfave.api.domain.user.repository.UserRepository;
import com.myfave.api.domain.user.entity.User;
import com.myfave.api.domain.saleevent.repository.SaleEventRepository;
import com.myfave.api.domain.saleevent.entity.SaleEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final SessionRegistry sessionRegistry;
    private final ChatMessageService chatMessageService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final SaleEventRepository saleEventRepository;

    @Value("${influencer.user-id}")
    private Long influencerUserId;

    public ChatRoomInfoResponse getChatRoomInfo() {
        ChatRoom chatRoom = chatRoomRepository.findByIsActiveTrue()
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        int participantCount = sessionRegistry.getParticipantCount(chatRoom.getChatRoomId());
        return ChatRoomInfoResponse.from(chatRoom, participantCount);
    }

    public ChatHistoryResponse getMessageHistory(int size, String before) {
        ChatRoom chatRoom = chatRoomRepository.findByIsActiveTrue()
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        ZonedDateTime cursor;
        try {
            cursor = before != null
                    ? ZonedDateTime.parse(before)
                    : ZonedDateTime.now().plusSeconds(1);
        } catch (java.time.format.DateTimeParseException e) {
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT);
        }

        List<StoredMessage> parsed = parseMessages(chatRoom.getChatRoomId(), cursor);

        int total = parsed.size();
        int fromIndex = Math.max(0, total - size);
        List<StoredMessage> page = parsed.subList(fromIndex, total);
        boolean hasMore = fromIndex > 0;

        List<ChatHistoryResponse.MessageItem> items = new ArrayList<>();
        for (StoredMessage msg : page) {
            StoredPayload p = msg.getPayload();
            items.add(ChatHistoryResponse.MessageItem.builder()
                    .messageId(p.getMessageId())
                    .senderId(p.getUserId())
                    .senderNickname(p.getNickname())
                    .influencer(Objects.equals(p.getUserId(), influencerUserId))
                    .content(p.getContent())
                    .createdAt(p.getSentAt())
                    .build());
        }

        return new ChatHistoryResponse(items, hasMore);
    }

    public ChatPreviewResponse getChatPreview(int size) {
        ChatRoom chatRoom = chatRoomRepository.findByIsActiveTrue()
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        int participantCount = sessionRegistry.getParticipantCount(chatRoom.getChatRoomId());

        List<StoredMessage> allParsed = parseMessages(chatRoom.getChatRoomId(), ZonedDateTime.now().plusSeconds(1));
        int total = allParsed.size();
        int fromIndex = Math.max(0, total - size);
        List<StoredMessage> page = allParsed.subList(fromIndex, total);

        List<ChatPreviewResponse.PreviewMessage> recentMessages = new ArrayList<>();
        for (StoredMessage msg : page) {
            StoredPayload p = msg.getPayload();
            recentMessages.add(ChatPreviewResponse.PreviewMessage.builder()
                    .senderNickname(p.getNickname())
                    .content(p.getContent())
                    .createdAt(p.getSentAt())
                    .build());
        }

        return new ChatPreviewResponse(chatRoom.getIsActive(), participantCount, recentMessages);
    }

    @Transactional
    public void openRoomForEvent(Long saleId) {
        SaleEvent saleEvent = saleEventRepository.findByIdWithLock(saleId)
                .orElseThrow(() -> new CustomException(ErrorCode.SALE_EVENT_NOT_FOUND));

        if (chatRoomRepository.findByIsActiveTrue().isPresent()) {
            log.info("채팅방 자동 개설 스킵: 이미 활성 채팅방 존재 (saleId={})", saleId);
            return;
        }

        User influencer = userRepository.findById(influencerUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        ChatRoom chatRoom = ChatRoom.builder()
                .user(influencer)
                .saleEvent(saleEvent)
                .build();

        chatRoomRepository.save(chatRoom);
        log.info("채팅방 자동 개설: saleId={}", saleId);
    }

    @Transactional
    public ChatRoomCloseResponse closeChatRoom(Long requestUserId) {
        ChatRoom chatRoom = chatRoomRepository.findTopByOrderByChatRoomIdDesc()
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (!chatRoom.getIsActive()) {
            throw new CustomException(ErrorCode.CHAT_ROOM_ALREADY_CLOSED);
        }

        if (!requestUserId.equals(influencerUserId)) {
            throw new CustomException(ErrorCode.AUTH_FORBIDDEN);
        }

        chatRoom.close();
        return ChatRoomCloseResponse.from(chatRoom);
    }

    List<StoredMessage> parseMessages(Long roomId, ZonedDateTime cursor) {
        List<String> rawMessages = chatMessageService.getHistory(roomId);
        List<StoredMessage> result = new ArrayList<>();

        for (String json : rawMessages) {
            try {
                StoredMessage msg = objectMapper.readValue(json, StoredMessage.class);
                if (msg.getPayload() == null) continue;
                if (!"NEW_MESSAGE".equals(msg.getType())) continue;
                if (!msg.getPayload().getSentAt().isBefore(cursor)) continue;
                result.add(msg);
            } catch (JsonProcessingException e) {
                log.warn("메시지 파싱 실패: {}", json);
            }
        }
        return result;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    static class StoredMessage {
        private String type;
        private StoredPayload payload;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    static class StoredPayload {
        private String messageId;
        private Long userId;
        private String nickname;
        private String content;
        private ZonedDateTime sentAt;
    }
}
