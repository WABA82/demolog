package com.examples.demolog.domains.postlike.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PostLikeEvent(
        String eventType,
        UUID postId,
        UUID postAuthorId,
        UUID actorId,
        LocalDateTime occurredAt
) {

    public static PostLikeEvent liked(UUID postId, UUID postAuthorId, UUID actorId) {
        return new PostLikeEvent("POST_LIKED", postId, postAuthorId, actorId, LocalDateTime.now());
    }

    public static PostLikeEvent unliked(UUID postId, UUID postAuthorId, UUID actorId) {
        return new PostLikeEvent("POST_UNLIKED", postId, postAuthorId, actorId, LocalDateTime.now());
    }
}
