package com.github.caetanoog18.conectatea.report.api;

import com.github.caetanoog18.conectatea.report.api.dto.GenerateStudentReportRequest;
import com.github.caetanoog18.conectatea.report.api.dto.StudentReportResponse;
import com.github.caetanoog18.conectatea.report.application.StudentReportService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/me/students/{studentId}/reports")
@PreAuthorize("isAuthenticated()")
public class StudentReportController {
    private final StudentReportService studentReportService;

    public StudentReportController(StudentReportService studentReportService) {
        this.studentReportService = studentReportService;
    }

    @PostMapping
    public ResponseEntity<StudentReportResponse> generate(
            @PathVariable UUID studentId,
            @Valid @RequestBody GenerateStudentReportRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var report = studentReportService.generate(studentId, request, jwt.getSubject());

        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(report);
    }
}