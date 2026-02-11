package com.examples.demolog.domains.postcomment.event;

import com.examples.demolog.domains.common.kafka.event.DomainEvent;

import java.time.LocalDateTime;
import java.util.UUID;

public record PostCommentEvent(
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
     * "게시물 댓글 생성" 이벤트
     */
    public static PostCommentEvent created(UUID postId, UUID postAuthorId, UUID actorId) {
        return new PostCommentEvent(
                EVENT_TOPIC,
                "POST_COMMENT_CREATED",
                AGGREGATE_TYPE,
                postId,
                postAuthorId,
                actorId,
                LocalDateTime.now()
        );
    }

    @Override
    public UUID aggregateId() {
        return postId;
    }

    @Override
    public LocalDateTime createdAt() {
        return occurredAt;
    }

}
