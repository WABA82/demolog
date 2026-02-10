package com.examples.demolog.domains.notification.model;

import com.examples.demolog.domains.notification.exception.NotificationErrorCode;
import com.examples.demolog.domains.notification.exception.NotificationException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "NOTIFICATION", indexes = {
    @Index(name = "idx_notification_receiver_id", columnList = "receiver_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Notification {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    // 알림 수신자
    @Column(name = "receiver_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID receiverId;

    //알림 발생시킨 사용자
    @Column(name = "actor_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID actorId;

    // 알림 타입
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    // 대상 리소스 ID
    @Column(name = "target_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID targetId;

    // 읽음 여부
    @Column(nullable = false)
    private boolean isRead;

    // 생성 시간
    private LocalDateTime createdAt;

    public static Notification create(UUID receiverId, UUID actorId, NotificationType type, UUID targetId) {
        return Notification.builder()
                .receiverId(receiverId)
                .actorId(actorId)
                .type(type)
                .targetId(targetId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public boolean isReceiver(UUID userId) {
        return this.receiverId.equals(userId);
    }

    public void validateReceiverOrThrow(UUID userId) {
        if (!isReceiver(userId)) {
            throw new NotificationException(NotificationErrorCode.FORBIDDEN_ACCESS);
        }
    }
}
