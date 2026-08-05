package com.example.pos.compliance.reference.repository;

import java.util.UUID;

import com.example.pos.compliance.reference.model.KraCodeList;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface KraCodeListRepository extends JpaRepository<KraCodeList, UUID> {
    Optional<KraCodeList> findByCodeTypeAndCodeValue(String codeType, String codeValue);
    List<KraCodeList> findByCodeTypeAndActiveTrue(String codeType);
}