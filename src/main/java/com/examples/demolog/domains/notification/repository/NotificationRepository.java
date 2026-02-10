package com.examples.demolog.domains.notification.repository;

import com.examples.demolog.domains.notification.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByReceiverId(UUID receiverId, Pageable pageable);

    long countByReceiverIdAndIsReadFalse(UUID receiverId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.receiverId = :receiverId AND n.isRead = false")
    int markAllAsReadByReceiverId(@Param("receiverId") UUID receiverId);
}
