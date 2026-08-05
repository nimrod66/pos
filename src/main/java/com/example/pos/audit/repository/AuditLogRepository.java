package com.example.pos.audit.repository;

import java.util.UUID;

import com.example.pos.audit.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByUserId(UUID userId);
    List<AuditLog> findByTableName(String tableName);
    List<AuditLog> findByTableNameAndRecordId(String tableName, String recordId);
}
