package com.examples.demolog.domains.notification.controller;

import com.examples.demolog.domains.notification.dto.response.NotificationResponse;
import com.examples.demolog.domains.notification.service.NotificationApplicationService;
import com.examples.demolog.global.response.ApiResponse;
import com.examples.demolog.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationApplicationService notificationApplicationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<NotificationResponse> response = notificationApplicationService.getNotifications(userDetails.getUserId(), pageable);
        return ApiResponse.ok(response);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        long count = notificationApplicationService.getUnreadCount(userDetails.getUserId());
        return ApiResponse.ok(count);
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        NotificationResponse response = notificationApplicationService.markAsRead(
                notificationId, userDetails.getUserId());
        return ApiResponse.ok(response);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        notificationApplicationService.markAllAsRead(userDetails.getUserId());
        return ApiResponse.noContent();
    }
}
