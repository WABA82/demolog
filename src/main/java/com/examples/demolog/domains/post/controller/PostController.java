package com.examples.demolog.domains.post.controller;

import com.examples.demolog.domains.post.dto.request.CreatePostRequest;
import com.examples.demolog.domains.post.dto.request.UpdatePostRequest;
import com.examples.demolog.domains.post.dto.response.PostResponse;
import com.examples.demolog.domains.post.service.PostApplicationService;
import com.examples.demolog.global.response.ApiResponse;
import com.examples.demolog.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
     * 게시물 목록 조회 (페이징)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getPosts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PostResponse> response = postApplicationService.getPosts(pageable);
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
