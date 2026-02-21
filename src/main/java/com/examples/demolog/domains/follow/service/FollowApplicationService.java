package com.examples.demolog.domains.follow.service;

import com.examples.demolog.domains.auth.repository.AppUserRepository;
import com.examples.demolog.domains.follow.dto.response.FollowResponse;
import com.examples.demolog.domains.follow.exception.FollowErrorCode;
import com.examples.demolog.domains.follow.exception.FollowException;
import com.examples.demolog.domains.follow.model.Follow;
import com.examples.demolog.domains.follow.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowApplicationService {

    private final FollowRepository followRepository;
    private final AppUserRepository appUserRepository;

    /**
     * 팔로우 (멱등성 보장)
     */
    @Transactional
    public FollowResponse follow(UUID followerId, UUID followingId) {
        if (followerId.equals(followingId)) {
            throw new FollowException(FollowErrorCode.CANNOT_FOLLOW_SELF);
        }

        if (!appUserRepository.existsById(followingId)) {
            throw new FollowException(FollowErrorCode.USER_NOT_FOUND);
        }

        return followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .map(FollowResponse::from)
                .orElseGet(() -> {
                    try {
                        Follow saved = followRepository.save(Follow.create(followerId, followingId));
                        return FollowResponse.from(saved);
                    } catch (DataIntegrityViolationException e) {
                        Optional<Follow> existing = followRepository.findByFollowerIdAndFollowingId(followerId, followingId);
                        if (existing.isPresent()) {
                            return FollowResponse.from(existing.get());
                        }
                        throw e;
                    }
                });
    }

    /**
     * 언팔로우 (멱등성 보장)
     */
    @Transactional
    public void unfollow(UUID followerId, UUID followingId) {
        if (!appUserRepository.existsById(followingId)) {
            throw new FollowException(FollowErrorCode.USER_NOT_FOUND);
        }

        followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
    }

    /**
     * 팔로우 여부 확인
     */
    public boolean isFollowing(UUID followerId, UUID followingId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    /**
     * 팔로잉 수 조회 (내가 팔로우하는 수)
     */
    public long getFollowingCount(UUID userId) {
        return followRepository.countByFollowerId(userId);
    }

    /**
     * 팔로워 수 조회 (나를 팔로우하는 수)
     */
    public long getFollowerCount(UUID userId) {
        return followRepository.countByFollowingId(userId);
    }

    /**
     * 팔로잉 목록 조회 - 내가 팔로우하는 사람들 (페이징)
     */
    public Page<FollowResponse> getFollowings(UUID userId, Pageable pageable) {
        Pageable pageableWithSort = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return followRepository.findByFollowerId(userId, pageableWithSort)
                .map(FollowResponse::from);
    }

    /**
     * 팔로워 목록 조회 - 나를 팔로우하는 사람들 (페이징)
     */
    public Page<FollowResponse> getFollowers(UUID userId, Pageable pageable) {
        Pageable pageableWithSort = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return followRepository.findByFollowingId(userId, pageableWithSort)
                .map(FollowResponse::from);
    }
}
