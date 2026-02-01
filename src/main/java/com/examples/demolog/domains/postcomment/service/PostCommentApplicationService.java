package com.examples.demolog.domains.postcomment.service;

import com.examples.demolog.domains.postcomment.dto.request.CreatePostCommentRequest;
import com.examples.demolog.domains.postcomment.dto.request.UpdatePostCommentRequest;
import com.examples.demolog.domains.postcomment.dto.response.PostCommentResponse;
import com.examples.demolog.domains.postcomment.exception.PostCommentErrorCode;
import com.examples.demolog.domains.postcomment.exception.PostCommentException;
import com.examples.demolog.domains.postcomment.model.PostComment;
import com.examples.demolog.domains.postcomment.repository.PostCommentRepository;
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
public class PostCommentApplicationService {

    private final PostCommentRepository postCommentRepository;

    /**
     * 댓글 생성
     */
    @Transactional
    public PostCommentResponse createPostComment(
            CreatePostCommentRequest createPostCommentRequest,
            UUID authorId
    ) {
        return PostCommentResponse.from(postCommentRepository.save(createPostCommentRequest.toEntity(authorId)));
    }

    /**
     * 댓글 조회
     */
    public PostCommentResponse getPostComment(UUID commentId) {
        return PostCommentResponse.from(
                postCommentRepository.findById(commentId).orElseThrow(() -> new PostCommentException(PostCommentErrorCode.NOT_FOUND))
        );
    }

    /**
     * 댓글 목록 조회
     */
    public Page<PostCommentResponse> getPostComments(Pageable pageable) {
        Pageable pageableWithSort = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return postCommentRepository.findAll(pageableWithSort)
                .map(PostCommentResponse::from);
    }

    /**
     * 댓글 수정
     */
    public PostCommentResponse update(
            UUID commentId,
            UpdatePostCommentRequest request,
            UUID authorId
    ) {
        PostComment postComment = postCommentRepository.findById(commentId).orElseThrow(() -> new PostCommentException(PostCommentErrorCode.NOT_FOUND));
        postComment.validateAuthorOrThrow(authorId);
        postComment.update(request.content());
        return PostCommentResponse.from(postComment);
    }

    /**
     * 댓글 삭제
     */
    @Transactional
    public void deletePostComment(UUID commentId) {
        postCommentRepository.deleteById(commentId);
    }

}
