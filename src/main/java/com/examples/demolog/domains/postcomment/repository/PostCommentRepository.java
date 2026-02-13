package com.examples.demolog.domains.postcomment.repository;

import com.examples.demolog.domains.postcomment.model.PostComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PostCommentRepository extends JpaRepository<PostComment, UUID>, PostCommentRepositoryCustom {
}
