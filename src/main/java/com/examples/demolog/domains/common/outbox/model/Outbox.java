package com.examples.demolog.domains.common.outbox.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "OUTBOX", indexes = {
    @Index(name = "idx_outbox_status_created_at", columnList = "status, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Outbox {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    // Kafka 토픽
    @Column(nullable = false, length = 100)
    private String topic;

    // 집계 타입 (예: "PostLike", "Post")
    @Column(nullable = false, length = 50)
    private String aggregateType;

    // 집계 ID (이벤트 발생 대상의 식별자)
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID aggregateId;

    // 이벤트 타입 (예: "POST_LIKED", "POST_COMMENTED")
    @Column(nullable = false, length = 50)
    private String eventType;

    // 이벤트 페이로드 (JSON)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    // 발행 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    // 생성 시각
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Kafka 발행 완료 시각
    private LocalDateTime processedAt;

    /**
     * 생성 메서드
     */
    public static Outbox create(String topic,
                                String aggregateType,
                                UUID aggregateId,
                                String eventType,
                                String payload
    ) {
        return Outbox.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .topic(topic)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 발행 완료 처리
     */
    public void markAsPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 발행 실패 처리
     */
    public void markAsFailed() {
        this.status = OutboxStatus.FAILED;
    }
}
