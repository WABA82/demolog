package com.examples.demolog.domains.notification.service;

import com.examples.demolog.domains.notification.dto.response.NotificationResponse;
import com.examples.demolog.domains.notification.exception.NotificationErrorCode;
import com.examples.demolog.domains.notification.exception.NotificationException;
import com.examples.demolog.domains.notification.model.Notification;
import com.examples.demolog.domains.notification.model.NotificationType;
import com.examples.demolog.domains.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationApplicationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public NotificationResponse createNotification(UUID receiverId, UUID actorId, NotificationType type, UUID targetId) {
        Notification notification = Notification.create(receiverId, actorId, type, targetId);
        Notification saved = notificationRepository.save(notification);
        return NotificationResponse.from(saved);
    }

    public Page<NotificationResponse> getNotifications(UUID receiverId, Pageable pageable) {
        Pageable pageableWithSort = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return notificationRepository.findByReceiverId(receiverId, pageableWithSort)
                .map(NotificationResponse::from);
    }

    public long getUnreadCount(UUID receiverId) {
        return notificationRepository.countByReceiverIdAndIsReadFalse(receiverId);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = findNotificationById(notificationId);
        notification.validateReceiverOrThrow(userId);
        notification.markAsRead();
        return NotificationResponse.from(notification);
    }

    @Transactional
    public void markAllAsRead(UUID receiverId) {
        notificationRepository.markAllAsReadByReceiverId(receiverId);
    }

    private Notification findNotificationById(UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOT_FOUND));
    }
}
