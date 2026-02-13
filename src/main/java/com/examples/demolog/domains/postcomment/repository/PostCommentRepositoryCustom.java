package com.examples.demolog.domains.postcomment.repository;

import com.examples.demolog.domains.postcomment.dto.response.PostCommentResponse;

import java.util.List;
import java.util.UUID;

public interface PostCommentRepositoryCustom {
    List<PostCommentResponse> findAllByPostIdAndCursor(UUID postId, UUID cursor, int size);
}
