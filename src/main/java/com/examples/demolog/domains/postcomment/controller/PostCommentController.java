package com.examples.demolog.domains.postcomment.controller;

import com.examples.demolog.domains.postcomment.dto.request.CreatePostCommentRequest;
import com.examples.demolog.domains.postcomment.dto.request.UpdatePostCommentRequest;
import com.examples.demolog.domains.postcomment.dto.response.PostCommentResponse;
import com.examples.demolog.domains.postcomment.service.PostCommentApplicationService;
import com.examples.demolog.global.response.ApiResponse;
import com.examples.demolog.global.security.CustomUserDetails;
import jakarta.validation.Valid;
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
@RequestMapping("/api/post-comments")
@RequiredArgsConstructor
public class PostCommentController {

    private final PostCommentApplicationService postCommentApplicationService;

    /**
     * 새 댓글 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PostCommentResponse>> createPost(
            @Valid @RequestBody CreatePostCommentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PostCommentResponse response = postCommentApplicationService.createPostComment(request, userDetails.getUserId());
        return ApiResponse.ok(response);
    }

    /**
     * 단일 댓글 조회
     */
    @GetMapping("/{commentId}")
    public ResponseEntity<ApiResponse<PostCommentResponse>> getPostComment(
            @PathVariable UUID commentId
    ) {
        PostCommentResponse response = postCommentApplicationService.getPostComment(commentId);
        return ApiResponse.ok(response);
    }

    /**
     * 댓글 목록 조회 (페이징)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostCommentResponse>>> getPosts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PostCommentResponse> response = postCommentApplicationService.getPostComments(pageable);
        return ApiResponse.ok(response);
    }

    /**
     * 댓글 수정
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<ApiResponse<PostCommentResponse>> updatePost(
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdatePostCommentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PostCommentResponse response = postCommentApplicationService.update(commentId, request, userDetails.getUserId());
        return ApiResponse.ok(response);
    }


}
