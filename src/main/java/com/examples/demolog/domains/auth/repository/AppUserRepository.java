package com.examples.demolog.domains.auth.repository;

import com.examples.demolog.domains.auth.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    boolean existsByEmail(String email);

    Optional<AppUser> findByEmail(String email);
}
