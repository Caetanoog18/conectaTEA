package com.github.caetanoog18.conectatea.report.application;

import com.github.caetanoog18.conectatea.audit.application.AuditedOperation;
import com.github.caetanoog18.conectatea.audit.domain.AuditAction;
import com.github.caetanoog18.conectatea.observation.api.dto.ObservationResponse;
import com.github.caetanoog18.conectatea.observation.infrastructure.ObservationRepository;
import com.github.caetanoog18.conectatea.report.api.dto.GenerateStudentReportRequest;
import com.github.caetanoog18.conectatea.report.api.dto.StudentReportResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class StudentReportService {
    private static final int MAX_OBSERVATIONS = 500;
    private static final Duration MAX_PERIOD = Duration.ofDays(366);

    private final ReportAccessService reportAccessService;
    private final ObservationRepository observationRepository;
    private final AuditedOperation auditedOperation;

    public StudentReportService(
            ReportAccessService reportAccessService,
            ObservationRepository observationRepository,
            AuditedOperation auditedOperation
    ) {
        this.reportAccessService = reportAccessService;
        this.observationRepository = observationRepository;
        this.auditedOperation = auditedOperation;
    }

    public StudentReportResponse generate(
            UUID studentId,
            GenerateStudentReportRequest request,
            String authenticatedEmail
    ) {
        return auditedOperation.execute(
                AuditAction.REPORT_GENERATE,
                studentId,
                null,
                authenticatedEmail,
                () -> {
                    var access = reportAccessService.requireGenerationAccess(studentId, authenticatedEmail);

                    validatePeriod(request);

                    var observations = observationRepository.findForReport(
                            studentId,
                            access.purpose(),
                            request.from(),
                            request.to(),
                            PageRequest.of(0, MAX_OBSERVATIONS + 1)
                    );

                    if (observations.size() > MAX_OBSERVATIONS) {
                        throw new ResponseStatusException(
                                HttpStatus.valueOf(422),
                                "Report exceeds 500 observations; select a shorter period"
                        );
                    }

                    var items = observations.stream()
                            .map(ObservationResponse::from)
                            .toList();

                    return new StudentReportResponse(
                            UUID.randomUUID(),
                            Instant.now(),
                            StudentReportResponse.StudentSummary.from(access.student()),
                            StudentReportResponse.RequesterSummary.from(access.requester()),
                            access.purpose(),
                            request.from(),
                            request.to(),
                            items.size(),
                            items
                    );
                },
                StudentReportResponse::reportId
        );
    }

    private static void validatePeriod(GenerateStudentReportRequest request) {
        if (request == null || request.from() == null || request.to() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start and end dates are required");
        }

        if (!request.from().isBefore(request.to())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date must be before end date");
        }

        if (Duration.between(request.from(), request.to()).compareTo(MAX_PERIOD) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Report period cannot exceed 366 days");
        }
    }
}