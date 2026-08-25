package com.example.pos.security.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthFailedLoginRepository extends JpaRepository<AuthFailedLogin, UUID> {
    Optional<AuthFailedLogin> findByEmail(String email);
}
