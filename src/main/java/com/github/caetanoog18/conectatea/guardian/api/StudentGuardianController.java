package com.github.caetanoog18.conectatea.guardian.api;

import com.github.caetanoog18.conectatea.guardian.api.dto.CreateStudentGuardianLinkRequest;
import com.github.caetanoog18.conectatea.guardian.api.dto.StudentGuardianResponse;
import com.github.caetanoog18.conectatea.guardian.api.dto.UpdateStudentGuardianLinkRequest;
import com.github.caetanoog18.conectatea.guardian.application.StudentGuardianService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
public class StudentGuardianController {
    private final StudentGuardianService service;
    public StudentGuardianController(StudentGuardianService service) {
        this.service = service;
    }

    @PostMapping("/students/{studentId}/guardians")
    public ResponseEntity<StudentGuardianResponse> create(
            @PathVariable UUID studentId,
            @Valid @RequestBody
            CreateStudentGuardianLinkRequest request
    ) {
        StudentGuardianResponse response = service.create(studentId, request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{guardianId}")
                .buildAndExpand(response.guardianId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/students/{studentId}/guardians")
    public ResponseEntity<List<StudentGuardianResponse>>
    findByStudent(@PathVariable UUID studentId) {
        return ResponseEntity.ok(
                service.findByStudent(studentId)
        );
    }

    @GetMapping("/guardians/{guardianId}/students")
    public ResponseEntity<List<StudentGuardianResponse>>
    findByGuardian(@PathVariable UUID guardianId) {
        return ResponseEntity.ok(
                service.findByGuardian(guardianId)
        );
    }

    @PutMapping(
            "/students/{studentId}/guardians/{guardianId}"
    )

    public ResponseEntity<StudentGuardianResponse> update(
            @PathVariable UUID studentId,
            @PathVariable UUID guardianId,
            @Valid @RequestBody
            UpdateStudentGuardianLinkRequest request
    ) {
        return ResponseEntity.ok(service.update(studentId, guardianId, request));
    }

    @DeleteMapping(
            "/students/{studentId}/guardians/{guardianId}"
    )
    public ResponseEntity<Void> delete(
            @PathVariable UUID studentId,
            @PathVariable UUID guardianId
    ) {
        service.delete(studentId, guardianId);
        return ResponseEntity.noContent().build();
    }
}