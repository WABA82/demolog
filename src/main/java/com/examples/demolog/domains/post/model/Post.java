package com.examples.demolog.domains.post.model;

import com.examples.demolog.domains.post.exception.PostErrorCode;
import com.examples.demolog.domains.post.exception.PostException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "POST")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Post {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID authorId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static Post create(String title, String content, UUID authorId) {
        LocalDateTime now = LocalDateTime.now();
        return Post.builder()
                .title(title)
                .content(content)
                .authorId(authorId)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isAuthor(UUID userId) {
        return this.authorId.equals(userId);
    }

    public void validateAuthorOrThrow(UUID userId) {
        if (!isAuthor(userId)) {
            throw new PostException(PostErrorCode.UNAUTHORIZED);
        }
    }
}
