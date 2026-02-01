package com.examples.demolog.domains.postcomment.dto.response;

import com.examples.demolog.domains.postcomment.model.PostComment;

import java.time.LocalDateTime;
import java.util.UUID;

public record PostCommentResponse(
        UUID id,
        String content,
        UUID postId,
        UUID authorId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static PostCommentResponse from(PostComment postComment) {
        return new PostCommentResponse(
                postComment.getId(),
                postComment.getContent(),
                postComment.getPostId(),
                postComment.getAuthorId(),
                postComment.getCreatedAt(),
                postComment.getUpdatedAt()
        );
    }

}
