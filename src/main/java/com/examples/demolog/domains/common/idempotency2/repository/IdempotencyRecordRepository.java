package com.examples.demolog.domains.common.idempotency2.repository;

import com.examples.demolog.domains.common.idempotency2.model.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

    Optional<IdempotencyRecord> findByIdempotencyKey(UUID key);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);

}
