package com.github.caetanoog18.conectatea.timeline.infrastructure;

import com.github.caetanoog18.conectatea.consent.domain.ConsentPurpose;
import com.github.caetanoog18.conectatea.observation.domain.Observation;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


public final class ObservationTimelineSpecification {
    private ObservationTimelineSpecification() {
    }

    public static Specification<Observation> authorizedPeriod(
            UUID studentId,
            ConsentPurpose purpose,
            Instant from,
            Instant to
    ) {
        Objects.requireNonNull(studentId, "Student is required");
        Objects.requireNonNull(purpose, "Purpose is required");

        return (root, query, builder) -> {
            if (query != null && Observation.class.equals(query.getResultType())) {
                root.fetch("author", JoinType.INNER);
            }

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(builder.equal(root.get("student").get("id"), studentId));

            predicates.add(builder.equal(root.get("purpose"), purpose));

            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.<Instant>get("occurredAt"), from));
            }

            if (to != null) {
                predicates.add(builder.lessThan(root.<Instant>get("occurredAt"), to));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}