package com.github.caetanoog18.conectatea.identity.infrastructure;

import com.github.caetanoog18.conectatea.identity.domain.User;
import com.github.caetanoog18.conectatea.identity.domain.UserRole;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByCpf(String cpf);
    Page<User> findByActiveTrueAndRoleIn(Collection<UserRole> roles, Pageable pageable);
}
