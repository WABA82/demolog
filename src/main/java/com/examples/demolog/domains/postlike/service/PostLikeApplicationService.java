package com.examples.demolog.domains.postlike.service;

import com.examples.demolog.domains.post.repository.PostRepository;
import com.examples.demolog.domains.postlike.dto.response.PostLikeResponse;
import com.examples.demolog.domains.postlike.exception.PostLikeErrorCode;
import com.examples.demolog.domains.postlike.exception.PostLikeException;
import com.examples.demolog.domains.postlike.model.PostLike;
import com.examples.demolog.domains.postlike.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeApplicationService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;

    /**
     * 게시물에 좋아요 추가 (멱등성 보장)
     */
    @Transactional
    public PostLikeResponse likePost(UUID postId, UUID userId) {
        // 게시글 존재 여부 확인
        if (!postRepository.existsById(postId)) {
            throw new PostLikeException(PostLikeErrorCode.POST_NOT_FOUND);
        }

        // 이미 좋아요한 경우 기존 데이터 반환 (멱등성)
        return postLikeRepository.findByPostIdAndUserId(postId, userId)
                .map(PostLikeResponse::from)
                .orElseGet(() -> {
                    try {
                        PostLike postLike = PostLike.create(postId, userId);
                        return PostLikeResponse.from(postLikeRepository.save(postLike));
                    } catch (DataIntegrityViolationException e) {
                        // 동시성으로 인한 중복 생성 시 기존 데이터 반환
                        PostLike existing = postLikeRepository.findByPostIdAndUserId(postId, userId)
                                .orElseThrow();
                        return PostLikeResponse.from(existing);
                    }
                });
    }

    /**
     * 게시물 좋아요 취소 (멱등성 보장)
     */
    @Transactional
    public void unlikePost(UUID postId, UUID userId) {
        // 게시글 존재 여부 확인
        if (!postRepository.existsById(postId)) {
            throw new PostLikeException(PostLikeErrorCode.POST_NOT_FOUND);
        }

        // deleteByPostIdAndUserId는 존재하지 않아도 예외 발생 없음 (멱등성)
        postLikeRepository.deleteByPostIdAndUserId(postId, userId);
    }

    /**
     * 게시물의 좋아요 개수 조회
     */
    public long getLikeCount(UUID postId) {
        // 게시글 존재 여부 확인
        if (!postRepository.existsById(postId)) {
            throw new PostLikeException(PostLikeErrorCode.POST_NOT_FOUND);
        }

        return postLikeRepository.countByPostId(postId);
    }

    /**
     * 사용자가 특정 게시물에 좋아요를 했는지 확인
     */
    public boolean isLikedByUser(UUID postId, UUID userId) {
        return postLikeRepository.existsByPostIdAndUserId(postId, userId);
    }

    /**
     * 게시물의 좋아요 목록 조회 (페이징)
     */
    public Page<PostLikeResponse> getPostLikes(UUID postId, Pageable pageable) {
        // 게시글 존재 여부 확인
        if (!postRepository.existsById(postId)) {
            throw new PostLikeException(PostLikeErrorCode.POST_NOT_FOUND);
        }

        Pageable pageableWithSort = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return postLikeRepository.findByPostId(postId, pageableWithSort)
                .map(PostLikeResponse::from);
    }
}
