package com.examples.demolog.domains.postlike.dto.response;

import com.examples.demolog.domains.postlike.model.PostLike;

import java.time.LocalDateTime;
import java.util.UUID;

public record PostLikeResponse(
        UUID id,
        UUID postId,
        UUID userId,
        LocalDateTime createdAt
) {

    public static PostLikeResponse from(PostLike postLike) {
        return new PostLikeResponse(
                postLike.getId(),
                postLike.getPostId(),
                postLike.getUserId(),
                postLike.getCreatedAt()
        );
    }

}
