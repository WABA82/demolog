package com.examples.demolog.domains.common.outbox.repository;

import com.examples.demolog.domains.common.outbox.model.Outbox;
import com.examples.demolog.domains.common.outbox.model.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<Outbox, UUID> {

    List<Outbox> findByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
