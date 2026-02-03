package com.examples.demolog.domains.postrevision.repository;

import com.examples.demolog.domains.postrevision.model.PostRevision;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PostRevisionRepository extends JpaRepository<PostRevision, UUID> {
    Page<PostRevision> findByPostId(UUID postId, Pageable pageable);

    Optional<PostRevision> findByPostIdAndRevisionNumber(UUID postId, int revisionNumber);

    int countByPostId(UUID postId);

    Optional<PostRevision> findTopByPostIdOrderByRevisionNumberDesc(UUID postId);
}
