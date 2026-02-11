package com.examples.demolog.domains.postlike.event;

import com.examples.demolog.domains.common.kafka.event.DomainEvent;

import java.time.LocalDateTime;
import java.util.UUID;

public record PostLikeEvent(
        String topic,
        String eventType,
        String aggregateType,
        UUID postId,
        UUID postAuthorId,
        UUID actorId,
        LocalDateTime occurredAt
) implements DomainEvent {

    private static final String EVENT_TOPIC = "post-like-events";
    private static final String AGGREGATE_TYPE = "PostLike";

    /**
     * "게시물 좋아요" 이벤트
     */
    public static PostLikeEvent liked(UUID postId, UUID postAuthorId, UUID actorId) {
        return new PostLikeEvent(EVENT_TOPIC, "POST_LIKED", AGGREGATE_TYPE, postId, postAuthorId, actorId, LocalDateTime.now());
    }

    /**
     * "게시물 좋아요 해제" 이벤트
     */
    public static PostLikeEvent unliked(UUID postId, UUID postAuthorId, UUID actorId) {
        return new PostLikeEvent(EVENT_TOPIC, "POST_UNLIKED", AGGREGATE_TYPE, postId, postAuthorId, actorId, LocalDateTime.now());
    }


    @Override
    public UUID aggregateId() {
        return this.postId;
    }

    @Override
    public LocalDateTime createdAt() {
        return occurredAt;
    }
}
