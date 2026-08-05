package com.example.pos.insurance.repository;

import java.util.UUID;

import com.example.pos.insurance.model.Authorization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthorizationRepository extends JpaRepository<Authorization, UUID> {
    Optional<Authorization> findByAuthorizationReference(String reference);
    List<Authorization> findByInsurerIdAndStatus(UUID insurerId, Authorization.AuthStatus status);
    List<Authorization> findByInsurerId(UUID insurerId);
}
