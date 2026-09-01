package com.github.caetanoog18.conectatea.guardian.api;

import com.github.caetanoog18.conectatea.guardian.api.dto.GuardianRequest;
import com.github.caetanoog18.conectatea.guardian.api.dto.GuardianResponse;
import com.github.caetanoog18.conectatea.guardian.api.dto.UpdateGuardianStatusRequest;
import com.github.caetanoog18.conectatea.guardian.application.GuardianService;
import com.github.caetanoog18.conectatea.shared.api.dto.PagedResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/guardians")
@PreAuthorize(
        "hasAnyRole('ADMINISTRATOR', 'PEDAGOGICAL_COORDINATOR')"
)
public class GuardianController {
    private final GuardianService guardianService;

    public GuardianController(GuardianService guardianService) {
        this.guardianService = guardianService;
    }

    @PostMapping
    public ResponseEntity<GuardianResponse> create(@Valid @RequestBody GuardianRequest request) {
        GuardianResponse response = guardianService.create(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<GuardianResponse>> findAll(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be zero or greater")
            int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must be at most 100")
            int size
    ) {
        return ResponseEntity.ok(guardianService.findAll(page, size));
    }

    @GetMapping("/{guardianId}")
    public ResponseEntity<GuardianResponse> findById(
            @PathVariable UUID guardianId
    ) {
        return ResponseEntity.ok(
                guardianService.findById(guardianId)
        );
    }

    @PutMapping("/{guardianId}")
    public ResponseEntity<GuardianResponse> update(
            @PathVariable UUID guardianId,
            @Valid @RequestBody GuardianRequest request
    ) {
        return ResponseEntity.ok(
                guardianService.update(guardianId, request)
        );
    }
    @PatchMapping("/{guardianId}/status")
    public ResponseEntity<GuardianResponse> updateStatus(
            @PathVariable UUID guardianId,
            @Valid @RequestBody UpdateGuardianStatusRequest request
    ) {
        return ResponseEntity.ok(guardianService.updateStatus(guardianId, request));
    }
}