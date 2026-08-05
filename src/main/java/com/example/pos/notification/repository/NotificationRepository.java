package com.example.pos.notification.repository;

import java.util.UUID;

import com.example.pos.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByBranchIdAndStatusOrderByCreatedAtDesc(UUID branchId, Notification.Status status);

    List<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, Notification.Status status);

    List<Notification> findByBranchIdOrderByCreatedAtDesc(UUID branchId);
}
