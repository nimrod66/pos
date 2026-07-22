package com.example.pos.insurance.repository;

import com.example.pos.insurance.model.ClaimAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClaimAttachmentRepository extends JpaRepository<ClaimAttachment, Long> {
    List<ClaimAttachment> findByClaimId(Long claimId);
}
