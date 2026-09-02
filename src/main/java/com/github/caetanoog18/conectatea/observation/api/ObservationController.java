package com.github.caetanoog18.conectatea.observation.api;

import com.github.caetanoog18.conectatea.observation.api.dto.CreateObservationRequest;
import com.github.caetanoog18.conectatea.observation.api.dto.ObservationResponse;
import com.github.caetanoog18.conectatea.observation.application.ObservationService;
import com.github.caetanoog18.conectatea.shared.api.dto.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/me/students/{studentId}/observations")
@PreAuthorize("isAuthenticated()")
public class ObservationController {
    private final ObservationService observationService;

    public ObservationController(ObservationService observationService) {
        this.observationService = observationService;
    }

    @PostMapping
    public ResponseEntity<ObservationResponse> create(
            @PathVariable UUID studentId,
            @Valid @RequestBody CreateObservationRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ObservationResponse response = observationService.create(studentId, request, jwt.getSubject());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<ObservationResponse>> findAll(
            @PathVariable UUID studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(observationService.findAll(studentId, page, size, jwt.getSubject()));
    }

    @GetMapping("/{observationId}")
    public ResponseEntity<ObservationResponse> findById(
            @PathVariable UUID studentId,
            @PathVariable UUID observationId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(observationService.findById(studentId, observationId, jwt.getSubject()));
    }
}