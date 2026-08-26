package com.example.pos.audit.repository;

import java.util.UUID;

import com.example.pos.audit.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @EntityGraph(attributePaths = {"user", "branch", "pharmacy"})
    Page<AuditLog> findByPharmacyId(UUID pharmacyId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "branch", "pharmacy"})
    Page<AuditLog> findByPharmacyIdAndTableName(UUID pharmacyId, String tableName, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "branch", "pharmacy"})
    Page<AuditLog> findByPharmacyIdAndTableNameAndRecordId(
            UUID pharmacyId, String tableName, String recordId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "branch", "pharmacy"})
    Page<AuditLog> findByPharmacyIdAndUserId(UUID pharmacyId, UUID userId, Pageable pageable);

    List<AuditLog> findByUserId(UUID userId);
    List<AuditLog> findByTableName(String tableName);
    List<AuditLog> findByTableNameAndRecordId(String tableName, String recordId);

    @org.springframework.data.jpa.repository.Query("select a from AuditLog a where a.pharmacy.id = :pharmacyId and a.createdAt >= :from and a.createdAt < :to order by a.createdAt desc")
    @EntityGraph(attributePaths = {"user", "branch", "pharmacy"})
    Page<AuditLog> findByPharmacyIdAndCreatedAtBetween(
            @Param("pharmacyId") UUID pharmacyId, @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to, Pageable pageable);
}
