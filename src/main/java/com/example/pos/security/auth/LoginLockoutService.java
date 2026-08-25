package com.example.pos.security.auth;

import com.example.pos.common.exception.ForbiddenException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Tracks failed sign-ins in its own transaction so the record survives the
 * rollback of the failed login attempt itself.
 */
@Service
public class LoginLockoutService {

    private static final int MAX_FAILED_LOGINS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final AuthFailedLoginRepository failedLoginRepository;

    public LoginLockoutService(AuthFailedLoginRepository failedLoginRepository) {
        this.failedLoginRepository = failedLoginRepository;
    }

    /** Throws when the email is locked out; silently passes otherwise. */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void enforceLockout(String normalizedEmail) {
        failedLoginRepository.findByEmail(normalizedEmail).ifPresent(record -> {
            int count = record.getFailureCount() == null ? 0 : record.getFailureCount();
            LocalDateTime cutoff = record.getLastFailure() == null
                    ? LocalDateTime.MIN
                    : record.getLastFailure().plusMinutes(LOCKOUT_MINUTES);
            if (count >= MAX_FAILED_LOGINS && LocalDateTime.now().isBefore(cutoff)) {
                long minutesLeft = Math.max(1, Duration.between(LocalDateTime.now(), cutoff).toMinutes());
                throw new ForbiddenException("Too many failed sign-in attempts. Try again in "
                        + minutesLeft + " minute(s).");
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String normalizedEmail) {
        AuthFailedLogin record = failedLoginRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> AuthFailedLogin.builder()
                        .email(normalizedEmail)
                        .failureCount(0)
                        .lastFailure(LocalDateTime.now())
                        .build());
        LocalDateTime now = LocalDateTime.now();
        boolean outsideWindow = record.getLastFailure() == null
                || record.getLastFailure().plusMinutes(LOCKOUT_MINUTES).isBefore(now);
        record.setFailureCount(outsideWindow ? 1 : record.getFailureCount() + 1);
        record.setLastFailure(now);
        failedLoginRepository.save(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clearFailures(String normalizedEmail) {
        failedLoginRepository.findByEmail(normalizedEmail)
                .ifPresent(failedLoginRepository::delete);
    }
}
