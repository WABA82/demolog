package com.examples.demolog.domains.postrevision.model;

import com.examples.demolog.domains.post.model.Post;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "POST_REVISION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class PostRevision {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID postId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID authorId;

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID modifiedBy;

    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Integer revisionNumber;

    public static PostRevision create(Post post, UUID modifiedBy, int revisionNumber) {
        return PostRevision.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorId(post.getAuthorId())
                .modifiedBy(modifiedBy)
                .revisionNumber(revisionNumber)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
