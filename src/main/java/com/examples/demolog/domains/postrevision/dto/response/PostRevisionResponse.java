package com.examples.demolog.domains.postrevision.dto.response;

import com.examples.demolog.domains.postrevision.model.PostRevision;

import java.time.LocalDateTime;
import java.util.UUID;

public record PostRevisionResponse(
        UUID id,
        UUID postId,
        String title,
        String content,
        UUID authorId,
        UUID modifiedBy,
        Integer revisionNumber,
        LocalDateTime createdAt
) {
    public static PostRevisionResponse from(PostRevision revision) {
        return new PostRevisionResponse(
                revision.getId(),
                revision.getPostId(),
                revision.getTitle(),
                revision.getContent(),
                revision.getAuthorId(),
                revision.getModifiedBy(),
                revision.getRevisionNumber(),
                revision.getCreatedAt()
        );
    }
}
