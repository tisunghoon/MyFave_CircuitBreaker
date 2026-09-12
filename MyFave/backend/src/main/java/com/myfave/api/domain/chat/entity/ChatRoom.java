package com.myfave.api.domain.chat.entity;

import com.myfave.api.domain.saleevent.entity.SaleEvent;
import com.myfave.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.ZonedDateTime;

@Entity
@Table(name = "chat_rooms")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long chatRoomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private SaleEvent saleEvent;

    @Column(nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "closed_at")
    private ZonedDateTime closedAt;

    @Builder
    private ChatRoom(User user, SaleEvent saleEvent) {
        this.user = user;
        this.saleEvent = saleEvent;
        this.isActive = true;
    }

    public void close() {
        this.isActive = false;
        this.closedAt = ZonedDateTime.now();
    }
}
