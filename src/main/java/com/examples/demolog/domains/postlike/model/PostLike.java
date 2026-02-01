package com.examples.demolog.domains.postlike.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "POST_LIKE", uniqueConstraints = {
    @UniqueConstraint(name = "uk_post_like_post_user", columnNames = {"post_id", "user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class PostLike {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "post_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID postId;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    private LocalDateTime createdAt;

    public static PostLike create(UUID postId, UUID userId) {
        LocalDateTime now = LocalDateTime.now();
        return PostLike.builder()
                .postId(postId)
                .userId(userId)
                .createdAt(now)
                .build();
    }
}
