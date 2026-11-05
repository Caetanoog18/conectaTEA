package com.github.caetanoog18.conectatea.audit.infrastructure;

import com.github.caetanoog18.conectatea.audit.domain.AuditEvent;
import com.github.caetanoog18.conectatea.audit.domain.AuditEventFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AuditEventQueryRepository {
    private final EntityManager entityManager;

    public AuditEventQueryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Page<AuditEvent> search(
            AuditEventFilter filter,
            Pageable pageable
    ) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        var contentQuery = builder.createQuery(AuditEvent.class);
        var contentRoot = contentQuery.from(AuditEvent.class);

        contentQuery
                .select(contentRoot)
                .where(predicates(filter, contentRoot, builder))
                .orderBy(builder.desc(contentRoot.get("occurredAt")), builder.desc(contentRoot.get("id")));

        List<AuditEvent> content = entityManager
                .createQuery(contentQuery)
                .setFirstResult(Math.toIntExact(pageable.getOffset()))
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        var countQuery = builder.createQuery(Long.class);
        var countRoot = countQuery.from(AuditEvent.class);

        countQuery.select(builder.count(countRoot)).where(predicates(filter, countRoot, builder));

        long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    private static Predicate[] predicates(
            AuditEventFilter filter,
            Root<AuditEvent> root,
            CriteriaBuilder builder
    ) {
        List<Predicate> predicates = new ArrayList<>();

        if (filter.studentId() != null) {
            predicates.add(builder.equal(root.get("studentId"), filter.studentId()));
        }

        if (filter.actorUserId() != null) {
            predicates.add(
                    builder.equal(root.get("actorUserId"), filter.actorUserId())
            );
        }

        if (filter.requestId() != null) {
            predicates.add(builder.equal(root.get("requestId"), filter.requestId()));
        }

        if (filter.action() != null) {
            predicates.add(builder.equal(root.get("action"), filter.action()));
        }

        if (filter.outcome() != null) {
            predicates.add(builder.equal(root.get("outcome"), filter.outcome()));
        }

        if (filter.from() != null) {
            predicates.add(builder.greaterThanOrEqualTo(root.<Instant>get("occurredAt"), filter.from()));
        }

        if (filter.to() != null) {
            predicates.add(builder.lessThan(root.<Instant>get("occurredAt"), filter.to()));
        }

        return predicates.toArray(Predicate[]::new);
    }
}