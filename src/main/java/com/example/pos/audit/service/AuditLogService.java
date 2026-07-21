package com.example.pos.audit.service;

import com.example.pos.audit.model.AuditLog;
import com.example.pos.audit.repository.AuditLogRepository;
import com.example.pos.user.users.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository repo;
    public AuditLogService(AuditLogRepository repo) { this.repo = repo; }

    public AuditLog log(User user, String tableName, String recordId, String action) {
        AuditLog audit = new AuditLog();
        audit.setUser(user);
        audit.setTableName(tableName);
        audit.setRecordId(recordId);
        audit.setAction(action);
        return repo.save(audit);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getByTable(String tableName, String recordId) {
        if (recordId != null) return repo.findByTableNameAndRecordId(tableName, recordId);
        return repo.findByTableName(tableName);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getByUser(Long userId) { return repo.findByUserId(userId); }
}
