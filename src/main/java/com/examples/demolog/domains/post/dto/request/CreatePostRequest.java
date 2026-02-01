package com.examples.demolog.domains.post.dto.request;

import com.examples.demolog.domains.post.model.Post;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreatePostRequest(
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 200, message = "제목은 200자 이하이어야 합니다.")
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        String content
) {
    public Post toEntity(UUID authorId) {
        return Post.create(title, content, authorId);
    }
}
