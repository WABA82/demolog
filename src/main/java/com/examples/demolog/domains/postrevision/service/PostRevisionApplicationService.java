package com.examples.demolog.domains.postrevision.service;

import com.examples.demolog.domains.post.dto.response.PostResponse;
import com.examples.demolog.domains.post.model.Post;
import com.examples.demolog.domains.post.repository.PostRepository;
import com.examples.demolog.domains.postrevision.dto.response.PostRevisionResponse;
import com.examples.demolog.domains.postrevision.exception.PostRevisionErrorCode;
import com.examples.demolog.domains.postrevision.exception.PostRevisionException;
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
public class PostRevisionApplicationService {

    private final PostRevisionRepository postRevisionRepository;
    private final PostRepository postRepository;

    public Page<PostRevisionResponse> getPostRevisions(UUID postId, Pageable pageable) {
        validatePostExists(postId);
        Pageable pageableWithSort = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "revisionNumber")
        );
        return postRevisionRepository.findByPostId(postId, pageableWithSort)
                .map(PostRevisionResponse::from);
    }

    @Transactional
    public PostResponse restoreRevision(UUID postId, UUID revisionId, UUID userId) {
        Post post = findPostById(postId);
        post.validateAuthorOrThrow(userId);

        PostRevision revision = findRevisionById(revisionId);

        if (!revision.getPostId().equals(postId)) {
            throw new PostRevisionException(PostRevisionErrorCode.NOT_FOUND);
        }

        int nextRevisionNumber = postRevisionRepository.countByPostId(postId) + 1;
        PostRevision currentStateRevision = PostRevision.create(post, userId, nextRevisionNumber);
        postRevisionRepository.save(currentStateRevision);

        post.update(revision.getTitle(), revision.getContent());

        return PostResponse.from(post);
    }

    private Post findPostById(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new PostRevisionException(PostRevisionErrorCode.POST_NOT_FOUND));
    }

    private PostRevision findRevisionById(UUID revisionId) {
        return postRevisionRepository.findById(revisionId)
                .orElseThrow(() -> new PostRevisionException(PostRevisionErrorCode.NOT_FOUND));
    }

    private void validatePostExists(UUID postId) {
        if (!postRepository.existsById(postId)) {
            throw new PostRevisionException(PostRevisionErrorCode.POST_NOT_FOUND);
        }
    }
}
