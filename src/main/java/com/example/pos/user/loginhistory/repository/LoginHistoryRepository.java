package com.example.pos.user.loginhistory.repository;

import java.util.UUID;

import com.example.pos.user.loginhistory.model.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, UUID> {

    List<LoginHistory> findByUserIdOrderByLoginTimeDesc(UUID userId);

    Optional<LoginHistory> findTopByUserIdOrderByLoginTimeDesc(UUID userId);
}
