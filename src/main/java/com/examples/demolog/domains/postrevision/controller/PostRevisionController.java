package com.examples.demolog.domains.postrevision.controller;

import com.examples.demolog.domains.post.dto.response.PostResponse;
import com.examples.demolog.domains.postrevision.dto.response.PostRevisionResponse;
import com.examples.demolog.domains.postrevision.service.PostRevisionApplicationService;
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
@RequestMapping("/api/posts/{postId}/revisions")
@RequiredArgsConstructor
public class PostRevisionController {

    private final PostRevisionApplicationService postRevisionApplicationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostRevisionResponse>>> getPostRevisions(
            @PathVariable UUID postId,
            @PageableDefault(size = 20, sort = "revisionNumber", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PostRevisionResponse> revisions = postRevisionApplicationService.getPostRevisions(postId, pageable);
        return ApiResponse.ok(revisions);
    }

    @PostMapping("/{revisionId}/restore")
    public ResponseEntity<ApiResponse<PostResponse>> restoreRevision(
            @PathVariable UUID postId,
            @PathVariable UUID revisionId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PostResponse response = postRevisionApplicationService.restoreRevision(
                postId,
                revisionId,
                userDetails.getUserId()
        );
        return ApiResponse.ok(response);
    }
}
