package com.example.pos.common.aop;

import com.example.pos.audit.model.AuditLog;
import com.example.pos.audit.repository.AuditLogRepository;
import com.example.pos.common.annotation.Auditable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
    private final AuditLogRepository auditLogRepository;

    public AuditAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        String action = auditable.action();
        String entity = auditable.entity();
        if (action.isEmpty()) action = joinPoint.getSignature().getName();
        if (entity.isEmpty()) entity = joinPoint.getTarget().getClass().getSimpleName();

        Object[] args = joinPoint.getArgs();
        Long recordId = args.length > 0 && args[0] instanceof Long ? (Long) args[0] : null;

        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - start;

        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setAction(action);
            auditLog.setTableName(entity);
            if (recordId != null) auditLog.setRecordId(String.valueOf(recordId));
            auditLog.setNewValue("Duration: " + duration + "ms");
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to persist audit log: {}", e.getMessage());
        }

        return result;
    }
}
