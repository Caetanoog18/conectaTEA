package com.github.caetanoog18.conectatea.identity.application;

import com.github.caetanoog18.conectatea.identity.api.dto.CreateUserRequest;
import com.github.caetanoog18.conectatea.shared.api.dto.PagedResponse;
import com.github.caetanoog18.conectatea.identity.api.dto.UpdateUserStatusRequest;
import com.github.caetanoog18.conectatea.identity.api.dto.UserResponse;
import com.github.caetanoog18.conectatea.identity.application.exception.EmailAlreadyInUseException;
import com.github.caetanoog18.conectatea.identity.application.exception.SelfDeactivationException;
import com.github.caetanoog18.conectatea.identity.application.exception.UserNotFoundException;
import com.github.caetanoog18.conectatea.identity.domain.User;
import com.github.caetanoog18.conectatea.identity.infrastructure.UserRepository;
import com.github.caetanoog18.conectatea.audit.application.AuditedOperation;
import com.github.caetanoog18.conectatea.audit.domain.AuditAction;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditedOperation auditedOperation;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditedOperation auditedOperation
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditedOperation = auditedOperation;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request, String authenticatedEmail) {
        return auditedOperation.execute(
                AuditAction.USER_CREATE,
                null,
                null,
                authenticatedEmail,
                () -> createUser(request),
                UserResponse::id
        );
    }

    private UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new EmailAlreadyInUseException();
        }

        User user = new User(
                request.fullName().trim(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.role()
        );

        try {
            User savedUser = userRepository.saveAndFlush(user);
            return UserResponse.from(savedUser);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyInUseException();
        }
    }

    public PagedResponse<UserResponse> findAll(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "fullName"));
        Page<UserResponse> users = userRepository.findAll(pageable).map(UserResponse::from);

        return PagedResponse.from(users);
    }

    public UserResponse findById(UUID userId) {
        return UserResponse.from(findUser(userId));
    }

    @Transactional
    public UserResponse updateStatus(UUID userId, UpdateUserStatusRequest request, String authenticatedEmail) {
        return auditedOperation.execute(
                AuditAction.USER_STATUS_UPDATE,
                null,
                userId,
                authenticatedEmail,
                () -> updateUserStatus(userId, request, authenticatedEmail), UserResponse::id);
    }

    private UserResponse updateUserStatus(UUID userId, UpdateUserStatusRequest request, String authenticatedEmail) {
        User user = findUser(userId);

        if (!request.active() && user.getEmail().equalsIgnoreCase(authenticatedEmail)) {
            throw new SelfDeactivationException();
        }

        if (request.active()) {
            user.activate();
        } else {
            user.deactivate();
        }

        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }
}