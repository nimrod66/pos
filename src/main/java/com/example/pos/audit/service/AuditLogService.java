package com.example.pos.audit.service;

import com.example.pos.audit.model.AuditLog;
import com.example.pos.audit.repository.AuditLogRepository;
import com.example.pos.user.users.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
    public Page<AuditLog> getByTable(String tableName, String recordId, Pageable pageable) {
        List<AuditLog> list;
        if (recordId != null) list = repo.findByTableNameAndRecordId(tableName, recordId);
        else list = repo.findByTableName(tableName);
        return new PageImpl<>(list, pageable, list.size());
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getByUser(Long userId, Pageable pageable) {
        List<AuditLog> list = repo.findByUserId(userId);
        return new PageImpl<>(list, pageable, list.size());
    }
}
