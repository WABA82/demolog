package com.examples.demolog.domains.post.repository;

import com.examples.demolog.domains.post.dto.response.PostFeedResponse;
import com.examples.demolog.domains.post.dto.response.PostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PostRepositoryCustom {
    Page<PostFeedResponse> findFeedOrderByLikeCount(Pageable pageable);

    List<PostResponse> findAllByCursor(UUID cursor, int size);
}
