package com.example.pos.user.loginhistory.repository;

import com.example.pos.user.loginhistory.model.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    List<LoginHistory> findByUserIdOrderByLoginTimeDesc(Long userId);

    Optional<LoginHistory> findTopByUserIdOrderByLoginTimeDesc(Long userId);
}
