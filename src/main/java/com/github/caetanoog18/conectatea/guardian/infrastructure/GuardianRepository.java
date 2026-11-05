package com.github.caetanoog18.conectatea.guardian.infrastructure;

import com.github.caetanoog18.conectatea.guardian.domain.Guardian;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GuardianRepository extends JpaRepository<Guardian, UUID> {
    Optional<Guardian> findByCpf(String cpf);
    boolean existsByCpf(String cpf);
    boolean existsByCpfAndIdNot(String cpf, UUID id);
    boolean existsByUserId(UUID userId);
    boolean existsByUserIdAndIdNot(UUID userId, UUID id);
}