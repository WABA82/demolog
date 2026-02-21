package com.examples.demolog.domains.follow.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "FOLLOW", uniqueConstraints = {
    @UniqueConstraint(name = "uk_follow_follower_following", columnNames = {"follower_id", "following_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Follow {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "follower_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID followerId;

    @Column(name = "following_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID followingId;

    private LocalDateTime createdAt;

    public static Follow create(UUID followerId, UUID followingId) {
        return Follow.builder()
                .followerId(followerId)
                .followingId(followingId)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
