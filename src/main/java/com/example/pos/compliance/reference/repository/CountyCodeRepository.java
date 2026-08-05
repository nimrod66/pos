package com.example.pos.compliance.reference.repository;

import java.util.UUID;

import com.example.pos.compliance.reference.model.CountyCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CountyCodeRepository extends JpaRepository<CountyCode, UUID> {
    Optional<CountyCode> findByCountyCode(String code);
}