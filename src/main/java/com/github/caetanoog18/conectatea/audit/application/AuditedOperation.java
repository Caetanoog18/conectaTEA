package com.github.caetanoog18.conectatea.audit.application;

import com.github.caetanoog18.conectatea.audit.domain.AuditAction;
import com.github.caetanoog18.conectatea.audit.domain.AuditEvent;
import com.github.caetanoog18.conectatea.audit.domain.AuditOutcome;
import com.github.caetanoog18.conectatea.audit.infrastructure.AuditEventRepository;
import com.github.caetanoog18.conectatea.identity.domain.User;
import com.github.caetanoog18.conectatea.identity.infrastructure.UserRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
public class AuditedOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditedOperation.class);
    private static final String REQUEST_ID_ATTRIBUTE = AuditedOperation.class.getName() + ".requestId";

    private final AuditEventRepository auditRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final TransactionTemplate operationTransaction;
    private final TransactionTemplate failureTransaction;

    public AuditedOperation(
            AuditEventRepository auditRepository,
            UserRepository userRepository,
            EntityManager entityManager,
            PlatformTransactionManager transactionManager
    ) {
        this.auditRepository = auditRepository;
        this.userRepository = userRepository;
        this.entityManager = entityManager;

        this.operationTransaction =
                new TransactionTemplate(transactionManager);

        this.operationTransaction.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRED
        );

        this.failureTransaction = new TransactionTemplate(transactionManager);
        this.failureTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public <T> T execute(
            AuditAction action,
            UUID studentId,
            UUID requestedResourceId,
            String authenticatedEmail,
            Supplier<T> operation,
            Function<T, UUID> resourceFromResult
    ) {
        UUID requestId = currentRequestId();

        AtomicReference<UUID> actorId = new AtomicReference<>();
        AtomicReference<UUID> resourceId = new AtomicReference<>(requestedResourceId);

        try {
            return operationTransaction.execute(transaction -> {
                if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
                    throw new IllegalStateException("Audited operations require a writable transaction");
                }

                actorId.set(resolveActorId(authenticatedEmail));

                T result = operation.get();

                UUID resolvedResourceId = resourceFromResult.apply(result);

                if (resolvedResourceId != null) {
                    resourceId.set(resolvedResourceId);
                }

                if (transaction.isRollbackOnly()) {
                    throw new UnexpectedRollbackException("Operation transaction was marked rollback-only");
                }

                auditRepository.save(
                        new AuditEvent(
                                actorId.get(),
                                action,
                                AuditOutcome.SUCCESS,
                                studentId,
                                resourceId.get(),
                                requestId
                        )
                );

                entityManager.flush();

                return result;
            });
        } catch (RuntimeException operationFailure) {
            AuditOutcome outcome = classify(operationFailure);

            try {
                failureTransaction.executeWithoutResult(transaction -> {
                    auditRepository.save(
                            new AuditEvent(
                                    actorId.get(),
                                    action,
                                    outcome,
                                    studentId,
                                    resourceId.get(),
                                    requestId
                            )
                    );

                    entityManager.flush();
                });
            } catch (RuntimeException auditFailure) {
                LOGGER.error("Audit persistence unavailable; requestId={}", requestId);
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Audit service unavailable");
            }

            throw operationFailure;
        }
    }

    private UUID resolveActorId(String authenticatedEmail) {
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            return null;
        }

        return userRepository
                .findByEmailIgnoreCase(authenticatedEmail)
                .map(User::getId)
                .orElse(null);
    }

    private static AuditOutcome classify(RuntimeException exception) {
        if (exception instanceof AccessDeniedException || exception instanceof AuthenticationException) {
            return AuditOutcome.DENIED;
        }

        if (exception instanceof ResponseStatusException responseException) {
            int status = responseException.getStatusCode().value();

            if (status == 401 || status == 403) {
                return AuditOutcome.DENIED;
            }
        }

        return AuditOutcome.FAILURE;
    }

    private static UUID currentRequestId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            var request = attributes.getRequest();
            Object existing = request.getAttribute(REQUEST_ID_ATTRIBUTE);

            UUID requestId = existing instanceof UUID value ? value : UUID.randomUUID();
            request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);

            var response = attributes.getResponse();

            if (response != null) {
                response.setHeader("X-Request-ID", requestId.toString());
            }

            return requestId;
        }

        return UUID.randomUUID();
    }
}