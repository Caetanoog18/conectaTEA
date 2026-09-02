package com.github.caetanoog18.conectatea.observation.infrastructure;

import com.github.caetanoog18.conectatea.consent.domain.ConsentPurpose;
import com.github.caetanoog18.conectatea.observation.domain.Observation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ObservationRepository extends JpaRepository<Observation, UUID>, JpaSpecificationExecutor<Observation> {
    Page<Observation> findAllByStudent_IdAndPurposeOrderByOccurredAtDescIdDesc(
            UUID studentId,
            ConsentPurpose purpose,
            Pageable pageable);

    Optional<Observation> findByIdAndStudent_IdAndPurpose(UUID observationId, UUID studentId, ConsentPurpose purpose);
}