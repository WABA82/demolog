package com.examples.demolog.domains.common.kafka.event;

import java.time.LocalDateTime;
import java.util.UUID;

public interface DomainEvent {

    String topic();

    String eventType();

    String aggregateType();

    UUID aggregateId();

    LocalDateTime createdAt();
}
