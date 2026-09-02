package com.github.caetanoog18.conectatea.careteam.api;

import com.github.caetanoog18.conectatea.careteam.application.StudentAccessService;
import com.github.caetanoog18.conectatea.student.api.dto.StudentResponse;
import com.github.caetanoog18.conectatea.student.domain.Student;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/me/students")
@PreAuthorize("hasAnyRole('TEACHER', 'AEE_TEACHER', " + "'PEDAGOGICAL_COORDINATOR', 'PSYCHOLOGIST', 'PHYSICIAN')")
public class ProfessionalStudentController {
    private final StudentAccessService studentAccessService;

    public ProfessionalStudentController(StudentAccessService studentAccessService) {
        this.studentAccessService = studentAccessService;
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<StudentResponse> findById(@PathVariable UUID studentId, @AuthenticationPrincipal Jwt jwt) {
        Student student = studentAccessService.requireProfileReadAccess(studentId, jwt.getSubject());

        return ResponseEntity.ok(StudentResponse.from(student));
    }
}