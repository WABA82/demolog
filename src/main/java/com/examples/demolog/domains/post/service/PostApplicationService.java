package com.examples.demolog.domains.post.service;

import com.examples.demolog.domains.post.dto.request.CreatePostRequest;
import com.examples.demolog.domains.post.dto.request.UpdatePostRequest;
import com.examples.demolog.domains.post.dto.response.PostFeedResponse;
import com.examples.demolog.domains.post.dto.response.PostResponse;
import com.examples.demolog.domains.post.exception.PostErrorCode;
import com.examples.demolog.domains.post.exception.PostException;
import com.examples.demolog.domains.post.model.Post;
import com.examples.demolog.domains.post.repository.PostRepository;
import com.examples.demolog.domains.postrevision.model.PostRevision;
import com.examples.demolog.domains.postrevision.repository.PostRevisionRepository;
import com.examples.demolog.global.response.CursorPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostApplicationService {

    private final PostRepository postRepository;
    private final PostRevisionRepository postRevisionRepository;

    @Transactional
    public PostResponse createPost(CreatePostRequest request, UUID authorId) {
        Post post = request.toEntity(authorId);
        Post saved = postRepository.save(post);
        return PostResponse.from(saved);
    }

    public PostResponse getPost(UUID postId) {
        Post post = findPostById(postId);
        return PostResponse.from(post);
    }

    public CursorPageResponse<PostResponse> getPostsByCursor(UUID cursor, int size) {
        List<PostResponse> posts = postRepository.findAllByCursor(cursor, size);
        UUID nextCursor = posts.size() > size ? posts.get(size - 1).id() : null;
        return CursorPageResponse.of(posts, size, nextCursor);
    }

    public Page<PostResponse> getPostsByOffset(Pageable pageable) {
        Pageable normalizedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return postRepository.findAllByOffset(normalizedPageable);
    }

    public Page<PostFeedResponse> getFeedPosts(Pageable pageable) {
        Pageable normalizedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return postRepository.findFeedOrderByLikeCount(normalizedPageable);
    }

    @Transactional
    public PostResponse updatePost(UUID postId, UpdatePostRequest request, UUID userId) {
        Post post = findPostById(postId);
        post.validateAuthorOrThrow(userId);

        // 게시물 수정 전 내용을 PostRevision로 저장
        int nextRevisionNumber = postRevisionRepository.countByPostId(postId) + 1;
        PostRevision revision = PostRevision.create(post, userId, nextRevisionNumber);
        postRevisionRepository.save(revision);

        // 게시물 업데이트
        post.update(request.title(), request.content());
        return PostResponse.from(post);
    }

    @Transactional
    public void deletePost(UUID postId, UUID userId) {
        Post post = findPostById(postId);
        post.validateAuthorOrThrow(userId);
        postRepository.delete(post);
    }

    private Post findPostById(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.NOT_FOUND));
    }
}
