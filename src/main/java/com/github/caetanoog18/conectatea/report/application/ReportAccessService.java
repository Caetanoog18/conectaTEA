package com.github.caetanoog18.conectatea.report.application;

import com.github.caetanoog18.conectatea.consent.domain.ConsentPurpose;
import com.github.caetanoog18.conectatea.consent.domain.ConsentStatus;
import com.github.caetanoog18.conectatea.consent.infrastructure.ConsentTermRepository;
import com.github.caetanoog18.conectatea.identity.domain.User;
import com.github.caetanoog18.conectatea.observation.application.ObservationAccessService;
import com.github.caetanoog18.conectatea.student.domain.Student;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReportAccessService {
    private final ObservationAccessService observationAccessService;
    private final ConsentTermRepository consentRepository;

    public ReportAccessService(
            ObservationAccessService observationAccessService,
            ConsentTermRepository consentRepository
    ) {
        this.observationAccessService = observationAccessService;
        this.consentRepository = consentRepository;
    }

    public ReportAccess requireGenerationAccess(UUID studentId, String authenticatedEmail) {
        var observationAccess = observationAccessService.requireReadAccess(studentId, authenticatedEmail);

        Instant now = Instant.now();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);

        long validConsents = consentRepository.countValidConsentsForReport(
                studentId,
                ConsentStatus.ACTIVE,
                now,
                today,
                observationAccess.purpose(),
                ConsentPurpose.INFORMATION_SHARING_WITH_CARE_TEAM,
                ConsentPurpose.REPORT_GENERATION
        );

        if (validConsents == 0) {
            throw new AccessDeniedException("Report generation is not authorized");
        }

        return new ReportAccess(
                observationAccess.student(),
                observationAccess.professional(),
                observationAccess.purpose());
    }

    public record ReportAccess(Student student, User requester, ConsentPurpose purpose) {
    }
}