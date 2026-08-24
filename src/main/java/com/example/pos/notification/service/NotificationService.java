package com.example.pos.notification.service;

import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.notification.model.Notification;
import com.example.pos.notification.repository.NotificationRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository repo;
    private final AuthenticatedUserContext current;

    public NotificationService(NotificationRepository repo, AuthenticatedUserContext current) {
        this.repo = repo;
        this.current = current;
    }

    public Notification create(String title, String message, Notification.Type type,
                               UUID branchId, UUID referenceId, String referenceType) {
        Notification n = Notification.builder()
                .title(title).message(message).type(type)
                .status(Notification.Status.UNREAD)
                .branchId(branchId).referenceId(referenceId).referenceType(referenceType).build();
        return repo.save(n);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getByBranch(UUID branchId, Pageable pageable) {
        UUID scopedBranchId = scopedBranch(branchId);
        return repo.findByBranchIdOrderByCreatedAtDesc(scopedBranchId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getUnreadByBranch(UUID branchId, Pageable pageable) {
        UUID scopedBranchId = scopedBranch(branchId);
        return repo.findByBranchIdAndStatusOrderByCreatedAtDesc(
                scopedBranchId, Notification.Status.UNREAD, pageable);
    }

    public Notification markRead(UUID id) {
        Notification n = scopedNotification(id);
        n.setStatus(Notification.Status.READ);
        return repo.save(n);
    }

    public void dismiss(UUID id) {
        Notification n = scopedNotification(id);
        n.setStatus(Notification.Status.DISMISSED);
        repo.save(n);
    }

    private UUID scopedBranch(UUID branchId) {
        UUID scoped = branchId == null ? current.branchId() : branchId;
        current.requireBranch(scoped);
        return scoped;
    }

    private Notification scopedNotification(UUID id) {
        return repo.findByIdAndBranchId(id, current.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
    }
}
