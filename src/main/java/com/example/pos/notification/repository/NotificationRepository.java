package com.example.pos.notification.repository;

import com.example.pos.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByBranchIdAndStatusOrderByCreatedAtDesc(Long branchId, Notification.Status status);

    List<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Notification.Status status);

    List<Notification> findByBranchIdOrderByCreatedAtDesc(Long branchId);
}
