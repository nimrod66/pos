package com.example.pos.notification.service;

import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.notification.model.Notification;
import com.example.pos.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository repo;
    public NotificationService(NotificationRepository repo) { this.repo = repo; }

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
        List<Notification> list = repo.findByBranchIdOrderByCreatedAtDesc(branchId);
        return new PageImpl<>(list, pageable, list.size());
    }

    @Transactional(readOnly = true)
    public Page<Notification> getUnreadByBranch(UUID branchId, Pageable pageable) {
        List<Notification> list = repo.findByBranchIdAndStatusOrderByCreatedAtDesc(branchId, Notification.Status.UNREAD);
        return new PageImpl<>(list, pageable, list.size());
    }

    public Notification markRead(UUID id) {
        Notification n = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        n.setStatus(Notification.Status.READ);
        return repo.save(n);
    }

    public void dismiss(UUID id) {
        Notification n = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        n.setStatus(Notification.Status.DISMISSED);
        repo.save(n);
    }
}
