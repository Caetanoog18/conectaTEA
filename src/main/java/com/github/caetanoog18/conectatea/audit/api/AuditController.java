package com.github.caetanoog18.conectatea.audit.api;

import com.github.caetanoog18.conectatea.audit.api.dto.AuditEventResponse;
import com.github.caetanoog18.conectatea.audit.application.AuditQueryService;
import com.github.caetanoog18.conectatea.audit.domain.AuditAction;
import com.github.caetanoog18.conectatea.audit.domain.AuditEventFilter;
import com.github.caetanoog18.conectatea.audit.domain.AuditOutcome;
import com.github.caetanoog18.conectatea.shared.api.dto.PagedResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit-events")
@PreAuthorize("isAuthenticated()")
public class AuditController {
    private final AuditQueryService auditQueryService;

    public AuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<AuditEventResponse>> search(
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) UUID requestId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditOutcome outcome,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,

            @AuthenticationPrincipal Jwt jwt
    ) {
        AuditEventFilter filter = new AuditEventFilter(
                studentId,
                actorUserId,
                requestId,
                action,
                outcome,
                from,
                to
        );

        return ResponseEntity.ok(auditQueryService.search(filter, page, size, jwt.getSubject()));
    }
}