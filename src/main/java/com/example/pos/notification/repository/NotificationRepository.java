package com.example.pos.notification.repository;

import java.util.UUID;

import com.example.pos.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByBranchIdAndStatusOrderByCreatedAtDesc(
            UUID branchId, Notification.Status status, Pageable pageable);

    Page<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(
            UUID userId, Notification.Status status, Pageable pageable);

    Page<Notification> findByBranchIdOrderByCreatedAtDesc(UUID branchId, Pageable pageable);

    Optional<Notification> findByIdAndBranchId(UUID id, UUID branchId);
}
