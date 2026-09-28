package com.github.caetanoog18.conectatea.consent.application;

import com.github.caetanoog18.conectatea.consent.api.dto.ConsentResponse;
import com.github.caetanoog18.conectatea.consent.api.dto.CreateConsentRequest;
import com.github.caetanoog18.conectatea.consent.api.dto.RevokeConsentRequest;
import com.github.caetanoog18.conectatea.consent.application.exception.ConsentConflictException;
import com.github.caetanoog18.conectatea.consent.application.exception.ConsentNotFoundException;
import com.github.caetanoog18.conectatea.consent.application.exception.InvalidConsentException;
import com.github.caetanoog18.conectatea.consent.domain.ConsentStatus;
import com.github.caetanoog18.conectatea.consent.domain.ConsentTerm;
import com.github.caetanoog18.conectatea.consent.infrastructure.ConsentTermRepository;
import com.github.caetanoog18.conectatea.guardian.domain.StudentGuardian;
import com.github.caetanoog18.conectatea.guardian.infrastructure.StudentGuardianRepository;
import com.github.caetanoog18.conectatea.identity.domain.User;
import com.github.caetanoog18.conectatea.identity.infrastructure.UserRepository;
import com.github.caetanoog18.conectatea.audit.application.AuditedOperation;
import com.github.caetanoog18.conectatea.audit.domain.AuditAction;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ConsentService {
    private final ConsentTermRepository consentRepository;
    private final StudentGuardianRepository linkRepository;
    private final UserRepository userRepository;
    private final AuditedOperation auditedOperation;

    public ConsentService(
            ConsentTermRepository consentRepository,
            StudentGuardianRepository linkRepository,
            UserRepository userRepository,
            AuditedOperation auditedOperation
    ) {
        this.consentRepository = consentRepository;
        this.linkRepository = linkRepository;
        this.userRepository = userRepository;
        this.auditedOperation = auditedOperation;
    }

    @Transactional
    public ConsentResponse create(UUID studentGuardianId, CreateConsentRequest request, String authenticatedEmail) {
        StudentGuardian link = findLink(studentGuardianId);

        UUID studentId = link.getStudent().getId();

        return auditedOperation.execute(
                AuditAction.CONSENT_CREATE,
                studentId,
                studentGuardianId,
                authenticatedEmail,
                () -> createConsent(link, request, authenticatedEmail), ConsentResponse::id);
    }

    private ConsentResponse createConsent(StudentGuardian link, CreateConsentRequest request, String authenticatedEmail) {
        validateLink(link);
        validateValidity(request);

        User recordedBy = findAuthenticatedUser(authenticatedEmail);

        UUID studentGuardianId = link.getId();

        expirePreviousConsent(studentGuardianId);

        if (consentRepository.existsByStudentGuardian_IdAndStatus(studentGuardianId, ConsentStatus.ACTIVE)) {
            throw new ConsentConflictException("Student guardian link already has " + "an active consent");
        }

        ConsentTerm consent = new ConsentTerm(
                link,
                request.purposes(),
                request.termsVersion().trim(),
                request.grantedAt(),
                request.validUntil(),
                recordedBy.getId()
        );

        try {
            return ConsentResponse.from(consentRepository.saveAndFlush(consent));
        } catch (DataIntegrityViolationException exception) {
            throw new ConsentConflictException("Unable to create consent because " + "of a data conflict");
        }
    }

    @Transactional
    public ConsentResponse findById(UUID consentId) {
        ConsentTerm consent = findConsent(consentId);
        consent.expireIfNecessary(currentDate());

        return ConsentResponse.from(consent);
    }

    public ConsentResponse findActive(UUID studentGuardianId) {
        findLink(studentGuardianId);

        ConsentTerm consent = consentRepository
                .findByStudentGuardian_IdAndStatus(studentGuardianId, ConsentStatus.ACTIVE)
                .orElseThrow(() -> new ConsentNotFoundException("Active consent not found for student guardian link"));

        if (consent.isExpired(currentDate())) {
            throw new ConsentNotFoundException("Active consent not found for student guardian link");
        }

        return ConsentResponse.from(consent);
    }

    @Transactional
    public List<ConsentResponse> findHistory(UUID studentGuardianId) {
        findLink(studentGuardianId);

        return consentRepository
                .findAllByStudentGuardian_IdOrderByGrantedAtDesc(studentGuardianId)
                .stream()
                .peek(consent -> consent.expireIfNecessary(currentDate()))
                .map(ConsentResponse::from)
                .toList();
    }

    @Transactional
    public ConsentResponse revoke(UUID consentId, RevokeConsentRequest request, String authenticatedEmail) {
        ConsentTerm consent = findConsent(consentId);

        UUID studentId = consent.getStudentGuardian().getStudent().getId();

        return auditedOperation.execute(
                AuditAction.CONSENT_REVOKE,
                studentId,
                consentId,
                authenticatedEmail,
                () -> revokeConsent(consent, request, authenticatedEmail), ConsentResponse::id);
    }

    private ConsentResponse revokeConsent(ConsentTerm consent, RevokeConsentRequest request, String authenticatedEmail) {
        if (consent.isExpired(currentDate())) {
            throw new ConsentConflictException("Expired consent cannot be revoked");
        }

        if (consent.getStatus() != ConsentStatus.ACTIVE) {
            throw new ConsentConflictException("Consent is not active");
        }

        User revokedBy = findAuthenticatedUser(authenticatedEmail);

        consent.revoke(java.time.Instant.now(), revokedBy.getId(), request.reason().trim());

        return ConsentResponse.from(consentRepository.saveAndFlush(consent));
    }

    private void expirePreviousConsent(UUID studentGuardianId) {
        consentRepository
                .findByStudentGuardian_IdAndStatus(studentGuardianId, ConsentStatus.ACTIVE)
                .ifPresent(consent -> {
                    if (consent.expireIfNecessary(currentDate())) {
                        consentRepository.saveAndFlush(consent);
                    }
                });
    }

    private void validateLink(StudentGuardian link) {
        if (!link.isLegalGuardian()) {
            throw new InvalidConsentException("Consent can only be registered for a legal guardian");
        }

        if (!link.getStudent().isActive() || !link.getGuardian().isActive()) {
            throw new InvalidConsentException("Student and guardian must be active");
        }
    }

    private void validateValidity(CreateConsentRequest request) {
        if (request.validUntil() == null) {
            return;
        }

        LocalDate grantedDate = request.grantedAt().atZone(ZoneOffset.UTC).toLocalDate();

        if (request.validUntil().isBefore(grantedDate)) {
            throw new InvalidConsentException("Valid until date cannot be before granted date");
        }
    }

    private StudentGuardian findLink(UUID studentGuardianId) {
        return linkRepository.findById(studentGuardianId)
                .orElseThrow(() -> new InvalidConsentException("Student guardian link not found"));
    }

    private ConsentTerm findConsent(UUID consentId) {
        return consentRepository.findById(consentId)
                .orElseThrow(() -> new ConsentNotFoundException(consentId));
    }

    private User findAuthenticatedUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found"));
    }

    private static LocalDate currentDate() {
        return LocalDate.now(ZoneOffset.UTC);
    }
}