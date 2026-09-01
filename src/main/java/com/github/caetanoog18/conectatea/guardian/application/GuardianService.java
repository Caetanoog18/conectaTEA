package com.github.caetanoog18.conectatea.guardian.application;

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


    public GuardianService(
            GuardianRepository guardianRepository,
            UserRepository userRepository
    ) {
        this.guardianRepository = guardianRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public GuardianResponse create(GuardianRequest request) {
        String normalizedCpf = normalizeDigits(request.cpf());

        validateCpf(normalizedCpf, null);
        validateUser(request.userId(), null);

        Guardian guardian = new Guardian(
                request.fullName(),
                request.cpf(),
                request.email(),
                request.phone(),
                request.userId()
        );

        return GuardianResponse.from(save(guardian));
    }

    public PagedResponse<GuardianResponse> findAll(int page, int size) {
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "fullName")
        );

        Page<GuardianResponse> guardians = guardianRepository
                .findAll(pageable)
                .map(GuardianResponse::from);

        return PagedResponse.from(guardians);
    }

    public GuardianResponse findById(UUID guardianId) {
        return GuardianResponse.from(findGuardian(guardianId));
    }

    @Transactional
    public GuardianResponse update(
            UUID guardianId,
            GuardianRequest request
    ) {
        Guardian guardian = findGuardian(guardianId);
        String normalizedCpf = normalizeDigits(request.cpf());

        validateCpf(normalizedCpf, guardianId);
        validateUser(request.userId(), guardianId);

        guardian.update(
                request.fullName(),
                request.cpf(),
                request.email(),
                request.phone(),
                request.userId()
        );

        return GuardianResponse.from(save(guardian));
    }

    @Transactional
    public GuardianResponse updateStatus(UUID guardianId, UpdateGuardianStatusRequest request) {
        Guardian guardian = findGuardian(guardianId);

        if (request.active()) {
            guardian.activate();
        } else {
            guardian.deactivate();
        }

        return GuardianResponse.from(
                guardianRepository.saveAndFlush(guardian)
        );
    }

    private Guardian findGuardian(UUID guardianId) {
        return guardianRepository.findById(guardianId)
                .orElseThrow(
                        () -> new GuardianNotFoundException(guardianId)
                );
    }

    private void validateCpf(String normalizedCpf, UUID currentGuardianId) {
        if (normalizedCpf == null) {
            return;
        }

        boolean exists = currentGuardianId == null
                ? guardianRepository.existsByCpf(normalizedCpf)
                : guardianRepository.existsByCpfAndIdNot(
                normalizedCpf,
                currentGuardianId
        );

        if (exists) {
            throw new CpfAlreadyInUseException();
        }
    }

    private void validateUser(UUID userId, UUID currentGuardianId) {
        if (userId == null) {
            return;
        }

        var user = userRepository.findById(userId)
                .orElseThrow(InvalidGuardianUserException::new);

        if (user.getRole() != UserRole.LEGAL_GUARDIAN) {
            throw new InvalidGuardianUserException();
        }

        boolean linked = currentGuardianId == null
                ? guardianRepository.existsByUserId(userId)
                : guardianRepository.existsByUserIdAndIdNot(
                userId,
                currentGuardianId
        );

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

    private static String normalizeDigits(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.replaceAll("\\D", "");
    }
}