package com.github.caetanoog18.conectatea.guardian.application;

import com.github.caetanoog18.conectatea.audit.application.AuditedOperation;
import com.github.caetanoog18.conectatea.audit.domain.AuditAction;
import com.github.caetanoog18.conectatea.guardian.api.dto.GuardianRequest;
import com.github.caetanoog18.conectatea.guardian.api.dto.GuardianResponse;
import com.github.caetanoog18.conectatea.guardian.api.dto.UpdateGuardianStatusRequest;
import com.github.caetanoog18.conectatea.guardian.application.exception.CpfAlreadyInUseException;
import com.github.caetanoog18.conectatea.guardian.application.exception.GuardianDataConflictException;
import com.github.caetanoog18.conectatea.guardian.application.exception.GuardianNotFoundException;
import com.github.caetanoog18.conectatea.guardian.application.exception.GuardianUserAlreadyLinkedException;
import com.github.caetanoog18.conectatea.guardian.application.exception.InvalidGuardianUserException;
import com.github.caetanoog18.conectatea.guardian.domain.Guardian;
import com.github.caetanoog18.conectatea.guardian.infrastructure.GuardianRepository;
import com.github.caetanoog18.conectatea.identity.domain.UserRole;
import com.github.caetanoog18.conectatea.identity.infrastructure.UserRepository;
import com.github.caetanoog18.conectatea.shared.api.dto.PagedResponse;
import com.github.caetanoog18.conectatea.shared.domain.Cpf;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GuardianService {
    private final GuardianRepository guardianRepository;
    private final UserRepository userRepository;
    private final AuditedOperation auditedOperation;

    public GuardianService(GuardianRepository guardianRepository, UserRepository userRepository, AuditedOperation auditedOperation) {
        this.guardianRepository = guardianRepository;
        this.userRepository = userRepository;
        this.auditedOperation = auditedOperation;
    }

    @Transactional
    public GuardianResponse create(GuardianRequest request, String authenticatedEmail) {
        return auditedOperation.execute(
                AuditAction.GUARDIAN_CREATE,
                null,
                null,
                authenticatedEmail,
                () -> createGuardian(request),
                GuardianResponse::id
        );
    }

    private GuardianResponse createGuardian(GuardianRequest request) {
        String normalizedCpf = Cpf.normalize(request.cpf());

        validateCpf(normalizedCpf, null);
        validateUser(request.userId(), null, normalizedCpf);

        Guardian guardian = new Guardian(
                request.fullName(),
                normalizedCpf,
                request.email(),
                request.phone(),
                request.userId()
        );

        return GuardianResponse.from(save(guardian));
    }

    public PagedResponse<GuardianResponse> findAll(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "fullName"));

        Page<GuardianResponse> guardians = guardianRepository
                .findAll(pageable)
                .map(GuardianResponse::from);

        return PagedResponse.from(guardians);
    }

    public GuardianResponse findById(UUID guardianId) {
        return GuardianResponse.from(findGuardian(guardianId));
    }

    @Transactional
    public GuardianResponse update(UUID guardianId, GuardianRequest request, String authenticatedEmail) {
        return auditedOperation.execute(
                AuditAction.GUARDIAN_UPDATE,
                null,
                guardianId,
                authenticatedEmail,
                () -> updateGuardian(guardianId, request),
                GuardianResponse::id
        );
    }

    private GuardianResponse updateGuardian(UUID guardianId, GuardianRequest request) {
        Guardian guardian = findGuardian(guardianId);
        String normalizedCpf = Cpf.normalize(request.cpf());

        validateCpf(normalizedCpf, guardianId);
        validateUser(request.userId(), guardianId, normalizedCpf);

        guardian.update(
                request.fullName(),
                normalizedCpf,
                request.email(),
                request.phone(),
                request.userId()
        );

        return GuardianResponse.from(save(guardian));
    }

    @Transactional
    public GuardianResponse updateStatus(UUID guardianId, UpdateGuardianStatusRequest request, String authenticatedEmail) {
        return auditedOperation.execute(
                AuditAction.GUARDIAN_STATUS_UPDATE,
                null,
                guardianId,
                authenticatedEmail,
                () -> updateGuardianStatus(guardianId, request),
                GuardianResponse::id
        );
    }

    private GuardianResponse updateGuardianStatus(UUID guardianId, UpdateGuardianStatusRequest request) {
        Guardian guardian = findGuardian(guardianId);

        if (request.active()) {
            guardian.activate();
        } else {
            guardian.deactivate();
        }

        return GuardianResponse.from(guardianRepository.saveAndFlush(guardian));
    }

    private Guardian findGuardian(UUID guardianId) {
        return guardianRepository.findById(guardianId).orElseThrow(() -> new GuardianNotFoundException(guardianId));
    }

    private void validateCpf(String normalizedCpf, UUID currentGuardianId) {
        boolean exists = currentGuardianId == null
                ? guardianRepository.existsByCpf(normalizedCpf)
                : guardianRepository.existsByCpfAndIdNot(
                normalizedCpf,
                currentGuardianId);

        if (exists) {
            throw new CpfAlreadyInUseException();
        }
    }

    private void validateUser(UUID userId, UUID currentGuardianId, String normalizedCpf) {
        if (userId == null) {
            return;
        }

        var user = userRepository.findById(userId).orElseThrow(InvalidGuardianUserException::new);

        if (user.getRole() != UserRole.LEGAL_GUARDIAN || !normalizedCpf.equals(user.getCpf())) {
            throw new InvalidGuardianUserException();
        }

        boolean linked = currentGuardianId == null
                ? guardianRepository.existsByUserId(userId)
                : guardianRepository.existsByUserIdAndIdNot(userId, currentGuardianId);

        if (linked) {
            throw new GuardianUserAlreadyLinkedException();
        }
    }

    private Guardian save(Guardian guardian) {
        try {
            return guardianRepository.saveAndFlush(guardian);
        } catch (DataIntegrityViolationException exception) {
            throw new GuardianDataConflictException();
        }
    }
}