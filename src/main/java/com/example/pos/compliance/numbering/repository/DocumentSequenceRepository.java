package com.example.pos.compliance.numbering.repository;

import com.example.pos.compliance.numbering.model.DocumentSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DocumentSequence> findByDocumentTypeAndBranchCodeAndSequenceDate(
            String documentType, String branchCode, String sequenceDate);
}
