package com.examples.demolog.domains.post.repository;

import com.examples.demolog.domains.post.dto.response.PostFeedResponse;
import com.examples.demolog.domains.post.model.Post;
import com.examples.demolog.domains.postlike.model.PostLike;
import com.examples.demolog.domains.postlike.repository.PostLikeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("PostRepository 테스트")
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    private UUID userId1;
    private UUID userId2;
    private UUID userId3;

    private Post post1;
    private Post post2;
    private Post post3;

    @BeforeEach
    void setUp() {
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();
        userId3 = UUID.randomUUID();

        // 게시물 생성 (시간 간격 포함)
        post1 = Post.create("Post 1", "Content 1", userId1);
        post2 = Post.create("Post 2", "Content 2", userId1);
        post3 = Post.create("Post 3", "Content 3", userId1);

        post1 = postRepository.save(post1);
        post2 = postRepository.save(post2);
        post3 = postRepository.save(post3);
    }

    @Nested
    @DisplayName("피드 조회 - findFeedOrderByLikeCount")
    class FindFeedOrderByLikeCount {

        @Test
        @DisplayName("좋아요 수가 많은 게시물이 먼저 나타난다")
        void shouldOrderByLikeCountDescending() {
            // Given: 게시물별 좋아요 추가
            // post1: 3개, post2: 1개, post3: 0개
            addLikes(post1, 3);
            addLikes(post2, 1);
            // post3는 좋아요 없음

            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<PostFeedResponse> result = postRepository.findFeedOrderByLikeCount(pageable);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result.getContent())
                    .extracting(PostFeedResponse::likeCount)
                    .containsExactly(3L, 1L, 0L);
        }

        @Test
        @DisplayName("같은 좋아요 수인 경우 최신순으로 정렬된다")
        void shouldOrderByCreatedAtWhenLikeCountIsSame() {
            // Given: 모든 게시물에 같은 좋아요 수
            addLikes(post1, 2);
            addLikes(post2, 2);
            addLikes(post3, 2);

            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<PostFeedResponse> result = postRepository.findFeedOrderByLikeCount(pageable);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result.getContent())
                    .extracting(PostFeedResponse::id)
                    .containsExactly(post3.getId(), post2.getId(), post1.getId());
        }

        @Test
        @DisplayName("좋아요가 없는 게시물도 포함된다 (LEFT JOIN)")
        void shouldIncludePostsWithoutLikes() {
            // Given: post1만 좋아요 추가
            addLikes(post1, 5);
            // post2, post3는 좋아요 없음

            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<PostFeedResponse> result = postRepository.findFeedOrderByLikeCount(pageable);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result.getContent())
                    .extracting(PostFeedResponse::id)
                    .containsExactlyInAnyOrder(post1.getId(), post2.getId(), post3.getId());
        }

        @Test
        @DisplayName("페이징이 올바르게 작동한다")
        void shouldPaginateCorrectly() {
            // Given: 5개의 게시물 생성
            for (int i = 0; i < 5; i++) {
                Post post = Post.create("Post " + i, "Content " + i, userId1);
                postRepository.save(post);
            }

            Pageable page1 = PageRequest.of(0, 3);

            // When
            Page<PostFeedResponse> result = postRepository.findFeedOrderByLikeCount(page1);

            // Then
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getNumberOfElements()).isEqualTo(3);
            assertThat(result.getTotalElements()).isEqualTo(8); // 처음 3개 + 새로 5개
            assertThat(result.getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("응답에 필요한 모든 필드가 포함된다")
        void shouldIncludeAllRequiredFields() {
            // Given
            addLikes(post1, 2);

            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<PostFeedResponse> result = postRepository.findFeedOrderByLikeCount(pageable);

            // Then
            PostFeedResponse response = result.getContent().stream()
                    .filter(r -> r.id().equals(post1.getId()))
                    .findFirst()
                    .orElseThrow();

            assertThat(response.id()).isNotNull();
            assertThat(response.title()).isNotNull();
            assertThat(response.content()).isNotNull();
            assertThat(response.authorId()).isNotNull();
            assertThat(response.createdAt()).isNotNull();
            assertThat(response.updatedAt()).isNotNull();
            assertThat(response.likeCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("빈 결과 집합을 반환한다")
        void shouldReturnEmptyPageWhenNoPostsExist() {
            // Given
            postRepository.deleteAll();

            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<PostFeedResponse> result = postRepository.findFeedOrderByLikeCount(pageable);

            // Then
            assertThat(result).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    /**
     * 게시물에 좋아요 추가 헬퍼 메서드
     */
    private void addLikes(Post post, int count) {
        for (int i = 0; i < count; i++) {
            UUID likerUserId = UUID.randomUUID();
            PostLike like = PostLike.create(post.getId(), likerUserId);
            postLikeRepository.save(like);
        }
    }
}
