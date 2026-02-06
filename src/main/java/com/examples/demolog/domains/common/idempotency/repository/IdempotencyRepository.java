package com.examples.demolog.domains.common.idempotency.repository;

import com.examples.demolog.domains.common.idempotency.model.Idempotency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyRepository extends JpaRepository<Idempotency, UUID> {

    Optional<Idempotency> findByIdempotencyToken(UUID key);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
