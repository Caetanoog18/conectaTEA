package com.github.caetanoog18.conectatea.audit.infrastructure;

import com.github.caetanoog18.conectatea.audit.domain.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditEventRepository extends Repository<AuditEvent, UUID> {
    AuditEvent save(AuditEvent event);
    Optional<AuditEvent> findById(UUID id);
    List<AuditEvent> findAllByRequestIdOrderByOccurredAtAscIdAsc(UUID requestId);
    Page<AuditEvent> findAllByStudentIdOrderByOccurredAtDescIdDesc(UUID studentId, Pageable pageable);
}