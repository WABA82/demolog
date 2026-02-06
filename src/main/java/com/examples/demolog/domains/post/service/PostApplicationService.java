package com.examples.demolog.domains.post.service;

import com.examples.demolog.domains.post.dto.request.CreatePostRequest;
import com.examples.demolog.domains.post.dto.request.UpdatePostRequest;
import com.examples.demolog.domains.post.dto.response.PostResponse;
import com.examples.demolog.domains.post.exception.PostErrorCode;
import com.examples.demolog.domains.post.exception.PostException;
import com.examples.demolog.domains.post.model.Post;
import com.examples.demolog.domains.post.repository.PostRepository;
import com.examples.demolog.domains.postrevision.model.PostRevision;
import com.examples.demolog.domains.postrevision.repository.PostRevisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public Page<PostResponse> getPosts(Pageable pageable) {
        Pageable pageableWithSort = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return postRepository.findAll(pageableWithSort)
                .map(PostResponse::from);
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
