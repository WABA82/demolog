package com.examples.demolog.domains.post.repository;

import com.examples.demolog.domains.post.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {
    Page<Post> findAll(Pageable pageable);
    Page<Post> findByAuthorId(UUID authorId, Pageable pageable);
}
