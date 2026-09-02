package com.github.caetanoog18.conectatea.observation.application;

import com.github.caetanoog18.conectatea.audit.application.AuditedOperation;
import com.github.caetanoog18.conectatea.audit.domain.AuditAction;
import com.github.caetanoog18.conectatea.observation.api.dto.CreateObservationRequest;
import com.github.caetanoog18.conectatea.observation.api.dto.ObservationResponse;
import com.github.caetanoog18.conectatea.observation.domain.Observation;
import com.github.caetanoog18.conectatea.observation.infrastructure.ObservationRepository;
import com.github.caetanoog18.conectatea.shared.api.dto.PagedResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class ObservationService {
    private final ObservationRepository observationRepository;
    private final ObservationAccessService accessService;
    private final AuditedOperation auditedOperation;

    public ObservationService(
            ObservationRepository observationRepository,
            ObservationAccessService accessService,
            AuditedOperation auditedOperation
    ) {
        this.observationRepository = observationRepository;
        this.accessService = accessService;
        this.auditedOperation = auditedOperation;
    }

    public ObservationResponse create(UUID studentId, CreateObservationRequest request, String authenticatedEmail) {
        return auditedOperation.execute(
                AuditAction.OBSERVATION_CREATE,
                studentId,
                null,
                authenticatedEmail,
                () -> {
                    var access = accessService.requireCreateAccess(studentId, authenticatedEmail);

                    Observation observation;

                    try {
                        observation = new Observation(
                                access.student(),
                                access.professional(),
                                access.purpose(),
                                request.title(),
                                request.content(),
                                request.occurredAt()
                        );
                    } catch (IllegalArgumentException exception) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid observation");
                    }

                    return ObservationResponse.from(observationRepository.saveAndFlush(observation));
                },
                ObservationResponse::id
        );
    }

    public PagedResponse<ObservationResponse> findAll(UUID studentId, int page, int size, String authenticatedEmail) {
        return auditedOperation.execute(
                AuditAction.OBSERVATION_LIST,
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

                    var result = observationRepository
                            .findAllByStudent_IdAndPurposeOrderByOccurredAtDescIdDesc(
                                    studentId,
                                    access.purpose(),
                                    PageRequest.of(page, size)
                            )
                            .map(ObservationResponse::from);

                    return PagedResponse.from(result);
                },
                result -> null
        );
    }

    public ObservationResponse findById(UUID studentId, UUID observationId, String authenticatedEmail) {
        return auditedOperation.execute(
                AuditAction.OBSERVATION_READ,
                studentId,
                observationId,
                authenticatedEmail,
                () -> {var access = accessService.requireReadAccess(studentId, authenticatedEmail);
                    Observation observation = observationRepository
                            .findByIdAndStudent_IdAndPurpose(
                                    observationId,
                                    studentId,
                                    access.purpose()
                            )
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                    "Observation not found"
                            ));

                    return ObservationResponse.from(observation);
                },
                ObservationResponse::id
        );
    }
}