package com.examples.demolog.domains.follow.controller;

import com.examples.demolog.domains.follow.dto.response.FollowResponse;
import com.examples.demolog.domains.follow.service.FollowApplicationService;
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
@RequestMapping("/api/users/{userId}")
@RequiredArgsConstructor
public class FollowController {

    private final FollowApplicationService followApplicationService;

    /**
     * 사용자 팔로우
     */
    @PostMapping("/follow")
    public ResponseEntity<ApiResponse<FollowResponse>> follow(
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        FollowResponse response = followApplicationService.follow(userDetails.getUserId(), userId);
        return ApiResponse.created(response);
    }

    /**
     * 사용자 언팔로우
     */
    @DeleteMapping("/follow")
    public ResponseEntity<ApiResponse<Void>> unfollow(
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        followApplicationService.unfollow(userDetails.getUserId(), userId);
        return ApiResponse.noContent();
    }

    /**
     * 팔로우 여부 확인 (현재 사용자가 userId를 팔로우하는지)
     */
    @GetMapping("/follow/me")
    public ResponseEntity<ApiResponse<Boolean>> isFollowing(
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        boolean isFollowing = followApplicationService.isFollowing(userDetails.getUserId(), userId);
        return ApiResponse.ok(isFollowing);
    }

    /**
     * 팔로잉 목록 조회 - userId가 팔로우하는 사람들
     */
    @GetMapping("/followings")
    public ResponseEntity<ApiResponse<Page<FollowResponse>>> getFollowings(
            @PathVariable UUID userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<FollowResponse> response = followApplicationService.getFollowings(userId, pageable);
        return ApiResponse.ok(response);
    }

    /**
     * 팔로잉 수 조회 - userId가 팔로우하는 수
     */
    @GetMapping("/followings/count")
    public ResponseEntity<ApiResponse<Long>> getFollowingCount(
            @PathVariable UUID userId
    ) {
        Long count = followApplicationService.getFollowingCount(userId);
        return ApiResponse.ok(count);
    }

    /**
     * 팔로워 목록 조회 - userId를 팔로우하는 사람들
     */
    @GetMapping("/followers")
    public ResponseEntity<ApiResponse<Page<FollowResponse>>> getFollowers(
            @PathVariable UUID userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<FollowResponse> response = followApplicationService.getFollowers(userId, pageable);
        return ApiResponse.ok(response);
    }

    /**
     * 팔로워 수 조회 - userId를 팔로우하는 수
     */
    @GetMapping("/followers/count")
    public ResponseEntity<ApiResponse<Long>> getFollowerCount(
            @PathVariable UUID userId
    ) {
        Long count = followApplicationService.getFollowerCount(userId);
        return ApiResponse.ok(count);
    }
}
