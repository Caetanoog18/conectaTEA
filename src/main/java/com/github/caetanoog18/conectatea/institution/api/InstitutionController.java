package com.github.caetanoog18.conectatea.institution.api;

import com.github.caetanoog18.conectatea.institution.api.dto.InstitutionRequest;
import com.github.caetanoog18.conectatea.institution.api.dto.InstitutionResponse;
import com.github.caetanoog18.conectatea.institution.application.InstitutionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/institution")
public class InstitutionController {
    private final InstitutionService institutionService;

    public InstitutionController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<InstitutionResponse> create(
            @Valid @RequestBody InstitutionRequest request
    ) {
        InstitutionResponse response =
                institutionService.create(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .build()
                .toUri();

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<InstitutionResponse> find() {
        return ResponseEntity.ok(institutionService.find());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<InstitutionResponse> update(
            @Valid @RequestBody InstitutionRequest request
    ) {
        return ResponseEntity.ok(
                institutionService.update(request)
        );
    }
}