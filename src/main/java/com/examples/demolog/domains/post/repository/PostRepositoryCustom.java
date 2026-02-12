package com.examples.demolog.domains.post.repository;

import com.examples.demolog.domains.post.dto.response.PostFeedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostRepositoryCustom {
    Page<PostFeedResponse> findFeedOrderByLikeCount(Pageable pageable);
}
