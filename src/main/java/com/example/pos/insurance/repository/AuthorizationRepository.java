package com.example.pos.insurance.repository;

import com.example.pos.insurance.model.Authorization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthorizationRepository extends JpaRepository<Authorization, Long> {
    Optional<Authorization> findByAuthorizationReference(String reference);
    List<Authorization> findByInsurerIdAndStatus(Long insurerId, Authorization.AuthStatus status);
    List<Authorization> findByInsurerId(Long insurerId);
}
