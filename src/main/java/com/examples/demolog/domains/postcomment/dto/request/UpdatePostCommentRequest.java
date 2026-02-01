package com.examples.demolog.domains.postcomment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePostCommentRequest(
        @NotBlank(message = "내용은 필수입니다.")
        @Size(max = 1000, message = "내용은 1000자 이하이어야 합니다.")
        String content
) {
}
