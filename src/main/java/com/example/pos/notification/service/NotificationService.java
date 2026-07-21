package com.example.pos.notification.service;

import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.notification.model.Notification;
import com.example.pos.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository repo;
    public NotificationService(NotificationRepository repo) { this.repo = repo; }

    public Notification create(String title, String message, Notification.Type type,
                               Long branchId, Long referenceId, String referenceType) {
        Notification n = Notification.builder()
                .title(title).message(message).type(type)
                .status(Notification.Status.UNREAD)
                .branchId(branchId).referenceId(referenceId).referenceType(referenceType).build();
        return repo.save(n);
    }

    @Transactional(readOnly = true)
    public List<Notification> getByBranch(Long branchId) {
        return repo.findByBranchIdOrderByCreatedAtDesc(branchId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadByBranch(Long branchId) {
        return repo.findByBranchIdAndStatusOrderByCreatedAtDesc(branchId, Notification.Status.UNREAD);
    }

    public Notification markRead(Long id) {
        Notification n = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        n.setStatus(Notification.Status.READ);
        return repo.save(n);
    }

    public void dismiss(Long id) {
        Notification n = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        n.setStatus(Notification.Status.DISMISSED);
        repo.save(n);
    }
}
