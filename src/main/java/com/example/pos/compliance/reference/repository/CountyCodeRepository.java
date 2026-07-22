package com.example.pos.compliance.reference.repository;

import com.example.pos.compliance.reference.model.CountyCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CountyCodeRepository extends JpaRepository<CountyCode, Long> {
    Optional<CountyCode> findByCountyCode(String code);
}