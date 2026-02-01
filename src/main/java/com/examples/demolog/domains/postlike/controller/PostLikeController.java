package com.examples.demolog.domains.postlike.controller;

import com.examples.demolog.domains.postlike.dto.response.PostLikeResponse;
import com.examples.demolog.domains.postlike.service.PostLikeApplicationService;
import com.examples.demolog.global.response.ApiResponse;
import com.examples.demolog.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/posts/{postId}/likes")
@RequiredArgsConstructor
public class PostLikeController {

    private final PostLikeApplicationService postLikeApplicationService;

    /**
     * 게시물에 좋아요 추가
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PostLikeResponse>> likePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PostLikeResponse response = postLikeApplicationService.likePost(postId, userDetails.getUserId());
        return ApiResponse.created(response);
    }

    /**
     * 게시물 좋아요 취소
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> unlikePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        postLikeApplicationService.unlikePost(postId, userDetails.getUserId());
        return ApiResponse.noContent();
    }

    /**
     * 게시물의 좋아요 목록 조회 (페이징)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostLikeResponse>>> getPostLikes(
            @PathVariable UUID postId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PostLikeResponse> response = postLikeApplicationService.getPostLikes(postId, pageable);
        return ApiResponse.ok(response);
    }

    /**
     * 게시물의 좋아요 개수 조회
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getLikeCount(
            @PathVariable UUID postId
    ) {
        Long count = postLikeApplicationService.getLikeCount(postId);
        return ApiResponse.ok(count);
    }

    /**
     * 현재 사용자가 해당 게시물에 좋아요를 했는지 확인
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Boolean>> isLikedByMe(
            @PathVariable UUID postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        boolean isLiked = postLikeApplicationService.isLikedByUser(postId, userDetails.getUserId());
        return ApiResponse.ok(isLiked);
    }

}
