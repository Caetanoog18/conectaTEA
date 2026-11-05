package com.github.caetanoog18.conectatea.audit.application;

import com.github.caetanoog18.conectatea.audit.api.dto.AuditEventResponse;
import com.github.caetanoog18.conectatea.audit.domain.AuditAction;
import com.github.caetanoog18.conectatea.audit.domain.AuditEventFilter;
import com.github.caetanoog18.conectatea.audit.infrastructure.AuditEventQueryRepository;
import com.github.caetanoog18.conectatea.identity.domain.User;
import com.github.caetanoog18.conectatea.identity.domain.UserRole;
import com.github.caetanoog18.conectatea.identity.infrastructure.UserRepository;
import com.github.caetanoog18.conectatea.shared.api.dto.PagedResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuditQueryService {
    private final AuditEventQueryRepository queryRepository;
    private final UserRepository userRepository;
    private final AuditedOperation auditedOperation;

    public AuditQueryService(
            AuditEventQueryRepository queryRepository,
            UserRepository userRepository,
            AuditedOperation auditedOperation
    ) {
        this.queryRepository = queryRepository;
        this.userRepository = userRepository;
        this.auditedOperation = auditedOperation;
    }

    public PagedResponse<AuditEventResponse> search(
            AuditEventFilter filter,
            int page,
            int size,
            String authenticatedEmail
    ) {
        return auditedOperation.execute(
                AuditAction.AUDIT_EVENTS_LIST,
                filter.studentId(),
                null,
                authenticatedEmail,
                () -> {
                    requireActiveAdministrator(authenticatedEmail);

                    if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination");
                    }

                    if (filter.from() != null && filter.to() != null && !filter.from().isBefore(filter.to())) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "From must be earlier than to"
                        );
                    }

                    var result = queryRepository
                            .search(filter, PageRequest.of(page, size))
                            .map(AuditEventResponse::from);

                    return PagedResponse.from(result);
                },
                result -> null
        );
    }

    private void requireActiveAdministrator(String authenticatedEmail) {
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw denied();
        }

        userRepository
                .findByEmailIgnoreCase(authenticatedEmail)
                .filter(User::isActive)
                .filter(user -> user.getRole() == UserRole.ADMINISTRATOR)
                .orElseThrow(AuditQueryService::denied);
    }

    private static AccessDeniedException denied() {
        return new AccessDeniedException("Access to audit events is not authorized");
    }
}