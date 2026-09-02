package com.github.caetanoog18.conectatea.consent.api;

import com.github.caetanoog18.conectatea.consent.api.dto.ConsentResponse;
import com.github.caetanoog18.conectatea.consent.api.dto.CreateConsentRequest;
import com.github.caetanoog18.conectatea.consent.api.dto.RevokeConsentRequest;
import com.github.caetanoog18.conectatea.consent.application.ConsentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PEDAGOGICAL_COORDINATOR')")
public class ConsentController {
    private final ConsentService consentService;
    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @PostMapping("/student-guardian-links/{linkId}/consents")
    public ResponseEntity<ConsentResponse> create(
            @PathVariable UUID linkId,
            @Valid @RequestBody CreateConsentRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ConsentResponse response = consentService.create(linkId, request, jwt.getSubject());

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/consents/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/consents/{consentId}")
    public ResponseEntity<ConsentResponse> findById(@PathVariable UUID consentId) {
        return ResponseEntity.ok(consentService.findById(consentId));
    }

    @GetMapping("/student-guardian-links/{linkId}/consents/active")
    public ResponseEntity<ConsentResponse> findActive(@PathVariable UUID linkId) {
        return ResponseEntity.ok(consentService.findActive(linkId));
    }

    @GetMapping("/student-guardian-links/{linkId}/consents")
    public ResponseEntity<List<ConsentResponse>> findHistory(
            @PathVariable UUID linkId
    ) {
        return ResponseEntity.ok(consentService.findHistory(linkId));
    }

    @PatchMapping("/consents/{consentId}/revoke")
    public ResponseEntity<ConsentResponse> revoke(
            @PathVariable UUID consentId,
            @Valid @RequestBody RevokeConsentRequest request,
            @AuthenticationPrincipal Jwt jwt)
    {
        return ResponseEntity.ok(
                consentService.revoke(
                        consentId,
                        request,
                        jwt.getSubject()
                )
        );
    }
}