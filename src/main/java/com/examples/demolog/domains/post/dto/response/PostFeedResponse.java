package com.examples.demolog.domains.post.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record PostFeedResponse(
        UUID id,
        String title,
        String content,
        UUID authorId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long likeCount
) {
}
