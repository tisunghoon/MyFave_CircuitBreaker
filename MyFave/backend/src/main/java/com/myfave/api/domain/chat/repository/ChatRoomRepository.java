package com.myfave.api.domain.chat.repository;

import com.myfave.api.domain.chat.entity.ChatRoom;
import com.myfave.api.domain.saleevent.entity.SaleEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 판매 이벤트의 활성화된 채팅방
    Optional<ChatRoom> findBySaleEventAndIsActiveTrue(SaleEvent saleEvent);

    // 활성화된 채팅방 조회
    Optional<ChatRoom> findByIsActiveTrue();

    // 최근 채팅방 조회 (isActive 무관) - 404 vs 409 구분용
    Optional<ChatRoom> findTopByOrderByChatRoomIdDesc();
}
