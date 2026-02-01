package com.examples.demolog.domains.postcomment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "COMMENT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class PostComment {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID postId;

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID authorId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static PostComment create(String content, UUID postId, UUID authorId) {
        LocalDateTime now = LocalDateTime.now();
        return PostComment.builder()
                .content(content)
                .postId(postId)
                .authorId(authorId)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void update(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isAuthor(UUID userId) {
        return this.authorId.equals(userId);
    }
}
