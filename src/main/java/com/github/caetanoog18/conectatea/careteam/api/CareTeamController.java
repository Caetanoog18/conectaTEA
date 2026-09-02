package com.github.caetanoog18.conectatea.careteam.api;

import com.github.caetanoog18.conectatea.careteam.api.dto.CreateProfessionalLinkRequest;
import com.github.caetanoog18.conectatea.careteam.api.dto.EndProfessionalLinkRequest;
import com.github.caetanoog18.conectatea.careteam.api.dto.ProfessionalLinkResponse;
import com.github.caetanoog18.conectatea.careteam.application.CareTeamService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PEDAGOGICAL_COORDINATOR')")
public class CareTeamController {
    private final CareTeamService careTeamService;

    public CareTeamController(CareTeamService careTeamService) {
        this.careTeamService = careTeamService;
    }

    @PostMapping("/students/{studentId}/care-team")
    public ResponseEntity<ProfessionalLinkResponse> create(
            @PathVariable UUID studentId,
            @Valid @RequestBody CreateProfessionalLinkRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ProfessionalLinkResponse response = careTeamService.create(studentId, request, jwt.getSubject());

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/care-team-links/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/students/{studentId}/care-team")
    public ResponseEntity<List<ProfessionalLinkResponse>> findByStudent(
            @PathVariable UUID studentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(careTeamService.findByStudent(studentId, jwt.getSubject()));
    }

    @GetMapping("/care-team-links/{linkId}")
    public ResponseEntity<ProfessionalLinkResponse> findById(
            @PathVariable UUID linkId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(careTeamService.findById(linkId, jwt.getSubject()));
    }

    @PatchMapping("/care-team-links/{linkId}/end")
    public ResponseEntity<ProfessionalLinkResponse> end(
            @PathVariable UUID linkId,
            @Valid @RequestBody EndProfessionalLinkRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(careTeamService.end(linkId, request, jwt.getSubject()));
    }
}