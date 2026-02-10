package com.examples.demolog.domains.notification.dto.response;

import com.examples.demolog.domains.notification.model.Notification;
import com.examples.demolog.domains.notification.model.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID receiverId,
        UUID actorId,
        NotificationType type,
        UUID targetId,
        boolean isRead,
        LocalDateTime createdAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getReceiverId(),
                notification.getActorId(),
                notification.getType(),
                notification.getTargetId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
