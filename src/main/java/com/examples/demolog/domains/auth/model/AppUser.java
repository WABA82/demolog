package com.examples.demolog.domains.auth.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "USER")
@Entity
@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME) // UUID v7 생성 전략
    @Column(columnDefinition = "BINARY(16)")         // MySQL에서 UUID 저장타입
    UUID id;

    @Column(nullable = false, unique = true)
    String email;

    @Column(nullable = false)
    String password;

    @Column(nullable = false, unique = true)
    String nickname;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;

    public static AppUser create(String email, String password, String nickname) {
        LocalDateTime now = LocalDateTime.now();
        return AppUser.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

}
