package com.github.caetanoog18.conectatea.observation.infrastructure;

import com.github.caetanoog18.conectatea.consent.domain.ConsentPurpose;
import com.github.caetanoog18.conectatea.observation.domain.Observation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ObservationRepository extends JpaRepository<Observation, UUID>, JpaSpecificationExecutor<Observation> {
    Page<Observation> findAllByStudent_IdAndPurposeOrderByOccurredAtDescIdDesc(
            UUID studentId,
            ConsentPurpose purpose,
            Pageable pageable
    );

    Optional<Observation> findByIdAndStudent_IdAndPurpose(
            UUID observationId,
            UUID studentId,
            ConsentPurpose purpose
    );

    @Query("""
            select o
            from Observation o
            join fetch o.author
            where o.student.id = :studentId
              and o.purpose = :purpose
              and o.occurredAt >= :from
              and o.occurredAt < :to
            order by o.occurredAt asc, o.id asc
            """)
    List<Observation> findForReport(
            @Param("studentId") UUID studentId,
            @Param("purpose") ConsentPurpose purpose,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}