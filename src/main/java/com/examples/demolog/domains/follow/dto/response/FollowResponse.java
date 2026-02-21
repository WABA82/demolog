package com.examples.demolog.domains.follow.dto.response;

import com.examples.demolog.domains.follow.model.Follow;

import java.time.LocalDateTime;
import java.util.UUID;

public record FollowResponse(
        UUID id,
        UUID followerId,
        UUID followingId,
        LocalDateTime createdAt
) {

    public static FollowResponse from(Follow follow) {
        return new FollowResponse(
                follow.getId(),
                follow.getFollowerId(),
                follow.getFollowingId(),
                follow.getCreatedAt()
        );
    }
}
