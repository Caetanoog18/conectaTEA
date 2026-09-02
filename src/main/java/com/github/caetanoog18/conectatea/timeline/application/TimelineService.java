package com.github.caetanoog18.conectatea.timeline.application;

import com.github.caetanoog18.conectatea.audit.application.AuditedOperation;
import com.github.caetanoog18.conectatea.audit.domain.AuditAction;
import com.github.caetanoog18.conectatea.observation.application.ObservationAccessService;
import com.github.caetanoog18.conectatea.observation.infrastructure.ObservationRepository;
import com.github.caetanoog18.conectatea.shared.api.dto.PagedResponse;
import com.github.caetanoog18.conectatea.timeline.api.dto.TimelineEventResponse;
import com.github.caetanoog18.conectatea.timeline.infrastructure.ObservationTimelineSpecification;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class TimelineService {
    private final ObservationRepository observationRepository;
    private final ObservationAccessService accessService;
    private final AuditedOperation auditedOperation;

    public TimelineService(
            ObservationRepository observationRepository,
            ObservationAccessService accessService,
            AuditedOperation auditedOperation
    ) {
        this.observationRepository = observationRepository;
        this.accessService = accessService;
        this.auditedOperation = auditedOperation;
    }

    public PagedResponse<TimelineEventResponse> findByStudent(
            UUID studentId,
            Instant from,
            Instant to,
            int page,
            int size,
            String authenticatedEmail
    ) {
        return auditedOperation.execute(
                AuditAction.TIMELINE_READ,
                studentId,
                null,
                authenticatedEmail,
                () -> {
                    var access = accessService.requireReadAccess(studentId, authenticatedEmail);

                    if (page < 0 || size < 1 || size > 100) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Page must be non-negative and size must be between 1 and 100"
                        );
                    }

                    if (from != null && to != null && !from.isBefore(to)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "From must be earlier than to");
                    }

                    var specification = ObservationTimelineSpecification.authorizedPeriod(
                            studentId,
                            access.purpose(),
                            from,
                            to
                    );

                    var sort = Sort.by(
                            Sort.Order.desc("occurredAt"),
                            Sort.Order.desc("id")
                    );

                    var result = observationRepository
                            .findAll(specification, PageRequest.of(page, size, sort))
                            .map(TimelineEventResponse::from);

                    return PagedResponse.from(result);
                },
                result -> null
        );
    }
}