package com.examples.demolog.domains.postcomment.dto.request;

import com.examples.demolog.domains.postcomment.model.PostComment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreatePostCommentRequest(
        @NotBlank(message = "댓글 내용은 필수입니다.")
        @Size(max = 1000, message = "댓글 내용은 1000자 이하이어야 합니다.")
        String content,
        @NotNull(message = "게시글 ID는 필수입니다.")
        UUID postId
) {

    public PostComment toEntity(UUID authorId) {
        return PostComment.create(content, postId, authorId);
    }

}
