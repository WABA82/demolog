package com.examples.demolog.domains.notification.consumer;

import com.examples.demolog.domains.notification.model.NotificationType;
import com.examples.demolog.domains.notification.service.NotificationApplicationService;
import com.examples.demolog.domains.postlike.event.PostLikeEvent;
import com.examples.demolog.global.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationApplicationService notificationApplicationService;

    @KafkaListener(topics = "post-like", groupId = "demolog-group")
    public void handlePostLikeEvent(ConsumerRecord<String, String> consumerRecord, Acknowledgment ack) {
        try {
            PostLikeEvent event = JsonUtil.fromJsonStr(consumerRecord.value(), PostLikeEvent.class);

            switch (event.eventType()) {
                case "POST_LIKED" -> {
                    notificationApplicationService.createNotification(
                            event.postAuthorId(),
                            event.actorId(),
                            NotificationType.POST_LIKED,
                            event.postId()
                    );
                    log.info("알림 생성 완료: type=POST_LIKED, postId={}, receiver={}", event.postId(), event.postAuthorId());
                }
                case "POST_UNLIKED" -> log.info("좋아요 해제 이벤트 수신: postId={}, actorId={}", event.postId(), event.actorId());
                default -> log.warn("알 수 없는 eventType: {}", event.eventType());
            }
        } catch (Exception e) {
            log.error("PostLikeEvent 처리 실패: {}", consumerRecord.value(), e);
        } finally {
            ack.acknowledge();
        }
    }
}
