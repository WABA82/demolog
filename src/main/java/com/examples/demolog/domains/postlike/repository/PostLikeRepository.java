package com.examples.demolog.domains.postlike.repository;

import com.examples.demolog.domains.postlike.model.PostLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PostLikeRepository extends JpaRepository<PostLike, UUID> {

    /**
     * postId와 userId로 좋아요 조회
     */
    Optional<PostLike> findByPostIdAndUserId(UUID postId, UUID userId);

    /**
     * postId와 userId로 좋아요 삭제
     */
    void deleteByPostIdAndUserId(UUID postId, UUID userId);

    /**
     * postId와 userId로 좋아요 존재 여부 확인
     */
    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    /**
     * postId로 좋아요 개수 조회
     */
    long countByPostId(UUID postId);

    /**
     * postId로 좋아요 목록 조회 (페이징)
     */
    Page<PostLike> findByPostId(UUID postId, Pageable pageable);
}
