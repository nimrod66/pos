package com.example.pos.audit.service;

import com.example.pos.audit.model.AuditLog;
import com.example.pos.audit.repository.AuditLogRepository;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.security.auth.AuthenticatedUserContext;
import com.example.pos.user.users.model.User;
import com.example.pos.user.users.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository repo;
    private final UserRepository userRepository;
    private final AuthenticatedUserContext current;

    public AuditLogService(AuditLogRepository repo,
                           UserRepository userRepository,
                           AuthenticatedUserContext current) {
        this.repo = repo;
        this.userRepository = userRepository;
        this.current = current;
    }

    public AuditLog log(User user, String tableName, String recordId, String action) {
        if (!user.getBranch().getPharmacy().getId().equals(current.pharmacy().getId())) {
            throw new ResourceNotFoundException("User", user.getId());
        }
        return repo.save(AuditLog.builder()
                .pharmacy(user.getBranch().getPharmacy())
                .branch(user.getBranch())
                .user(user)
                .tableName(tableName)
                .recordId(recordId)
                .action(action)
                .build());
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAll(Pageable pageable) {
        return repo.findByPharmacyId(current.pharmacy().getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getByTable(String tableName, String recordId, Pageable pageable) {
        UUID pharmacyId = current.pharmacy().getId();
        return recordId == null
                ? repo.findByPharmacyIdAndTableName(pharmacyId, tableName, pageable)
                : repo.findByPharmacyIdAndTableNameAndRecordId(pharmacyId, tableName, recordId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getByUser(UUID userId, Pageable pageable) {
        userRepository.findByIdAndBranchPharmacyId(userId, current.pharmacy().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return repo.findByPharmacyIdAndUserId(current.pharmacy().getId(), userId, pageable);
    }
}
