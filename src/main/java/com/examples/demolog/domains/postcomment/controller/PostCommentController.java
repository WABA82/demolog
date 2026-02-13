package com.examples.demolog.domains.postcomment.controller;

import com.examples.demolog.domains.common.idempotency.annotation.Idempotent;
import com.examples.demolog.domains.postcomment.dto.request.CreatePostCommentRequest;
import com.examples.demolog.domains.postcomment.dto.request.UpdatePostCommentRequest;
import com.examples.demolog.domains.postcomment.dto.response.PostCommentResponse;
import com.examples.demolog.domains.postcomment.service.PostCommentApplicationService;
import com.examples.demolog.global.response.ApiResponse;
import com.examples.demolog.global.response.CursorPageResponse;
import com.examples.demolog.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class PostCommentController {

    private final PostCommentApplicationService postCommentApplicationService;

    /**
     * 새 댓글 생성
     */
    @Idempotent
    @PostMapping
    public ResponseEntity<ApiResponse<PostCommentResponse>> createPostComment(
            @PathVariable UUID postId,
            @Valid @RequestBody CreatePostCommentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PostCommentResponse response = postCommentApplicationService.createPostComment(postId, request, userDetails.getUserId());
        return ApiResponse.ok(response);
    }

    /**
     * 댓글 목록 조회 (커서 기반 페이지네이션)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<PostCommentResponse>>> getPostComments(
            @PathVariable UUID postId,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        CursorPageResponse<PostCommentResponse> response = postCommentApplicationService.getPostCommentsByCursor(postId, cursor, size);
        return ApiResponse.ok(response);
    }

    /**
     * 댓글 수정
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<ApiResponse<PostCommentResponse>> updatePostComment(
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdatePostCommentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PostCommentResponse response = postCommentApplicationService.update(postId, commentId, request, userDetails.getUserId());
        return ApiResponse.ok(response);
    }

    /**
     * 댓글 삭제
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deletePostComment(
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        postCommentApplicationService.deletePostComment(postId, commentId, userDetails.getUserId());
        return ApiResponse.noContent();
    }

}
