package com.examples.demolog.domains.post.dto.response;

import com.examples.demolog.domains.post.model.Post;

import java.time.LocalDateTime;
import java.util.UUID;

public record PostResponse(
        UUID id,
        String title,
        String content,
        UUID authorId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthorId(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
