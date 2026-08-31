package com.github.caetanoog18.conectatea.institution.application;

import com.github.caetanoog18.conectatea.institution.api.dto.InstitutionRequest;
import com.github.caetanoog18.conectatea.institution.api.dto.InstitutionResponse;
import com.github.caetanoog18.conectatea.institution.application.exception.InstitutionAlreadyExistsException;
import com.github.caetanoog18.conectatea.institution.application.exception.InstitutionNotFoundException;
import com.github.caetanoog18.conectatea.institution.domain.Institution;
import com.github.caetanoog18.conectatea.institution.infrastructure.InstitutionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InstitutionService {
    private final InstitutionRepository institutionRepository;

    public InstitutionService(
            InstitutionRepository institutionRepository
    ) {
        this.institutionRepository = institutionRepository;
    }

    @Transactional
    public InstitutionResponse create(InstitutionRequest request) {
        if (institutionRepository.count() > 0) {
            throw new InstitutionAlreadyExistsException();
        }

        Institution institution = createInstitution(request);

        try {
            Institution savedInstitution =
                    institutionRepository.saveAndFlush(institution);

            return InstitutionResponse.from(savedInstitution);
        } catch (DataIntegrityViolationException exception) {
            throw new InstitutionAlreadyExistsException();
        }
    }

    public InstitutionResponse find() {
        return InstitutionResponse.from(findInstitution());
    }

    @Transactional
    public InstitutionResponse update(InstitutionRequest request) {
        Institution institution = findInstitution();
        institution.update(
                request.name(),
                request.taxId(),
                request.email(),
                request.phone(),
                request.street(),
                request.addressNumber(),
                request.complement(),
                request.district(),
                request.city(),
                request.state(),
                request.postalCode()
        );

        Institution updatedInstitution =
                institutionRepository.saveAndFlush(institution);
        return InstitutionResponse.from(updatedInstitution);
    }

    private Institution findInstitution() {
        return institutionRepository
                .findFirstByOrderByCreatedAtAsc()
                .orElseThrow(InstitutionNotFoundException::new);
    }

    private Institution createInstitution(InstitutionRequest request) {
        return new Institution(
                request.name(),
                request.taxId(),
                request.email(),
                request.phone(),
                request.street(),
                request.addressNumber(),
                request.complement(),
                request.district(),
                request.city(),
                request.state(),
                request.postalCode()
        );
    }
}