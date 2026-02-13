package com.examples.demolog.domains.postcomment.service;

import com.examples.demolog.domains.post.model.Post;
import com.examples.demolog.domains.post.repository.PostRepository;
import com.examples.demolog.domains.postcomment.dto.request.CreatePostCommentRequest;
import com.examples.demolog.domains.postcomment.dto.request.UpdatePostCommentRequest;
import com.examples.demolog.domains.postcomment.dto.response.PostCommentResponse;
import com.examples.demolog.domains.postcomment.event.PostCommentOutboxWriter;
import com.examples.demolog.domains.postcomment.exception.PostCommentErrorCode;
import com.examples.demolog.domains.postcomment.exception.PostCommentException;
import com.examples.demolog.domains.postcomment.model.PostComment;
import com.examples.demolog.domains.postcomment.repository.PostCommentRepository;
import com.examples.demolog.global.response.CursorPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostCommentApplicationService {

    private final PostCommentRepository postCommentRepository;

    private final PostRepository postRepository;

    private final PostCommentOutboxWriter outboxWriter;

    /**
     * 댓글 생성
     */
    @Transactional
    public PostCommentResponse createPostComment(
            UUID postId,
            CreatePostCommentRequest createPostCommentRequest,
            UUID authorId
    ) {
        // 게시글 존재 여부 확인
        Post post = postRepository.findById(postId).orElseThrow(() -> new PostCommentException(PostCommentErrorCode.POST_NOT_FOUND));

        PostComment saved = postCommentRepository.save(createPostCommentRequest.toEntity(postId, authorId));
        PostCommentResponse response = PostCommentResponse.from(postCommentRepository.save(saved));

        outboxWriter.savePostCommentCreatedEvent(post, authorId);
        return response;
    }

    /**
     * 댓글 목록 조회 (커서 기반 페이지네이션)
     */
    public CursorPageResponse<PostCommentResponse> getPostCommentsByCursor(UUID postId, UUID cursor, int size) {
        // 게시글 존재 여부 확인
        if (!postRepository.existsById(postId)) {
            throw new PostCommentException(PostCommentErrorCode.POST_NOT_FOUND);
        }

        List<PostCommentResponse> comments = postCommentRepository.findAllByPostIdAndCursor(postId, cursor, size);
        UUID nextCursor = comments.size() > size ? comments.get(size - 1).id() : null;
        return CursorPageResponse.of(comments, size, nextCursor);
    }

    /**
     * 댓글 수정
     */
    public PostCommentResponse update(
            UUID postId,
            UUID commentId,
            UpdatePostCommentRequest request,
            UUID authorId
    ) {
        // 게시글 존재 여부 확인
        if (postRepository.existsById(postId)) {
            throw new PostCommentException(PostCommentErrorCode.POST_NOT_FOUND);
        }

        // 댓글 존재 여부 확인 및 작성자 검증
        PostComment postComment = postCommentRepository.findById(commentId).orElseThrow(() -> new PostCommentException(PostCommentErrorCode.NOT_FOUND));
        postComment.validateAuthorOrThrow(authorId);

        // 댓글 수정
        postComment.update(request.content());
        return PostCommentResponse.from(postComment);
    }

    /**
     * 댓글 삭제
     */
    @Transactional
    public void deletePostComment(UUID postId, UUID commentId, UUID userId) {
        // 게시글 존재 여부 확인
        if (postRepository.existsById(postId)) {
            throw new PostCommentException(PostCommentErrorCode.POST_NOT_FOUND);
        }

        // 댓글 존재 여부 확인 및 작성자 검증
        PostComment postComment = postCommentRepository.findById(commentId).orElseThrow(() -> new PostCommentException(PostCommentErrorCode.NOT_FOUND));
        postComment.validateAuthorOrThrow(userId);

        // 댓글 삭제
        postCommentRepository.delete(postComment);
    }
}
