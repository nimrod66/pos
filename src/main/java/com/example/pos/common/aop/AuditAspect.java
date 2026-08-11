package com.example.pos.common.aop;

import com.example.pos.audit.model.AuditLog;
import com.example.pos.audit.repository.AuditLogRepository;
import com.example.pos.common.BaseEntity;
import com.example.pos.common.annotation.Auditable;
import com.example.pos.security.auth.AuthenticatedUserContext;
import com.example.pos.user.users.model.User;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
    private final AuditLogRepository auditLogRepository;
    private final AuthenticatedUserContext current;

    public AuditAspect(AuditLogRepository auditLogRepository,
                       AuthenticatedUserContext current) {
        this.auditLogRepository = auditLogRepository;
        this.current = current;
    }

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        long startedAt = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - startedAt;

        try {
            User user = current.user();
            String action = auditable.action().isEmpty()
                    ? joinPoint.getSignature().getName() : auditable.action();
            String entity = auditable.entity().isEmpty()
                    ? joinPoint.getTarget().getClass().getSimpleName() : auditable.entity();
            AuditLog auditLog = AuditLog.builder()
                    .pharmacy(user.getBranch().getPharmacy())
                    .branch(user.getBranch())
                    .user(user)
                    .action(action)
                    .tableName(entity)
                    .recordId(resolveRecordId(joinPoint.getArgs(), result))
                    .newValue("Duration: " + duration + "ms")
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception ex) {
            log.warn("Failed to persist audit log: {}", ex.getMessage());
        }
        return result;
    }

    private String resolveRecordId(Object[] args, Object result) {
        if (result instanceof BaseEntity entity && entity.getId() != null) {
            return entity.getId().toString();
        }
        return Arrays.stream(args)
                .filter(UUID.class::isInstance)
                .map(UUID.class::cast)
                .map(UUID::toString)
                .findFirst()
                .orElse(null);
    }
}
