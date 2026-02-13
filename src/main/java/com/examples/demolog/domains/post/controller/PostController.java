package com.examples.demolog.domains.post.controller;

import com.examples.demolog.domains.common.idempotency.annotation.Idempotent;
import com.examples.demolog.domains.post.dto.request.CreatePostRequest;
import com.examples.demolog.domains.post.dto.request.UpdatePostRequest;
import com.examples.demolog.domains.post.dto.response.PostFeedResponse;
import com.examples.demolog.domains.post.dto.response.PostResponse;
import com.examples.demolog.domains.post.service.PostApplicationService;
import com.examples.demolog.global.response.ApiResponse;
import com.examples.demolog.global.response.CursorPageResponse;
import com.examples.demolog.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostApplicationService postApplicationService;

    /**
     * 새 게시물 생성
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @Valid @RequestBody CreatePostRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PostResponse response = postApplicationService.createPost(request, userDetails.getUserId());
        return ApiResponse.created(response);
    }

    /**
     * 단일 게시물 조회
     */
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(
            @PathVariable UUID postId
    ) {
        PostResponse response = postApplicationService.getPost(postId);
        return ApiResponse.ok(response);
    }

    /**
     * 게시물 목록 조회 (커서 기반 페이지네이션)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<PostResponse>>> getPosts(
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        CursorPageResponse<PostResponse> response = postApplicationService.getPostsByCursor(cursor, size);
        return ApiResponse.ok(response);
    }

    /**
     * 인기 게시물 피드 조회 (좋아요 수 기반 정렬)
     */
    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<Page<PostFeedResponse>>> getFeedPosts(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<PostFeedResponse> response = postApplicationService.getFeedPosts(pageable);
        return ApiResponse.ok(response);
    }

    /**
     * 게시물 수정
     */
    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable UUID postId,
            @Valid @RequestBody UpdatePostRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PostResponse response = postApplicationService.updatePost(postId, request, userDetails.getUserId());
        return ApiResponse.ok(response);
    }

    /**
     * 게시물 삭제
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        postApplicationService.deletePost(postId, userDetails.getUserId());
        return ApiResponse.noContent();
    }
}
