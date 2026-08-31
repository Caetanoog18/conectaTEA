package com.github.caetanoog18.conectatea.student.api;

import com.github.caetanoog18.conectatea.shared.api.dto.PagedResponse;
import com.github.caetanoog18.conectatea.student.api.dto.StudentRequest;
import com.github.caetanoog18.conectatea.student.api.dto.StudentResponse;
import com.github.caetanoog18.conectatea.student.api.dto.UpdateStudentStatusRequest;
import com.github.caetanoog18.conectatea.student.application.StudentService;
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
@RequestMapping("/api/students")
@PreAuthorize(
        "hasAnyRole('ADMINISTRATOR', 'PEDAGOGICAL_COORDINATOR')"
)
public class StudentController {
    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    public ResponseEntity<StudentResponse> create(
            @Valid @RequestBody StudentRequest request
    ) {
        StudentResponse response = studentService.create(request);

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
    public ResponseEntity<PagedResponse<StudentResponse>> findAll(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be zero or greater")
            int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must be at most 100")
            int size
    ) {
        return ResponseEntity.ok(
                studentService.findAll(page, size)
        );
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<StudentResponse> findById(
            @PathVariable UUID studentId
    ) {
        return ResponseEntity.ok(
                studentService.findById(studentId)
        );
    }

    @PutMapping("/{studentId}")
    public ResponseEntity<StudentResponse> update(
            @PathVariable UUID studentId,
            @Valid @RequestBody StudentRequest request
    ) {
        return ResponseEntity.ok(
                studentService.update(studentId, request)
        );
    }

    @PatchMapping("/{studentId}/status")
    public ResponseEntity<StudentResponse> updateStatus(
            @PathVariable UUID studentId,
            @Valid @RequestBody UpdateStudentStatusRequest request
    ) {
        return ResponseEntity.ok(
                studentService.updateStatus(studentId, request)
        );
    }
}