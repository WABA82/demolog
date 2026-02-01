package com.examples.demolog.domains.post.repository;

import com.examples.demolog.domains.post.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {
}
