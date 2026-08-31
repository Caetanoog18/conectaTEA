package com.github.caetanoog18.conectatea.identity.api;

import com.github.caetanoog18.conectatea.identity.api.dto.CreateUserRequest;
import com.github.caetanoog18.conectatea.shared.api.dto.PagedResponse;
import com.github.caetanoog18.conectatea.identity.api.dto.UpdateUserStatusRequest;
import com.github.caetanoog18.conectatea.identity.api.dto.UserResponse;
import com.github.caetanoog18.conectatea.identity.application.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(
            @Valid @RequestBody CreateUserRequest request
    ) {
        UserResponse response = userService.create(request);

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
    public ResponseEntity<PagedResponse<UserResponse>> findAll(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be zero or greater")
            int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must be at most 100")
            int size
    ) {
        return ResponseEntity.ok(userService.findAll(page, size));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> findById(
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(userService.findById(userId));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<UserResponse> updateStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                userService.updateStatus(
                        userId,
                        request,
                        jwt.getSubject()
                )
        );
    }
}