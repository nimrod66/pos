package com.example.pos.user.loginhistory.repository;

import java.util.UUID;

import com.example.pos.user.loginhistory.model.LoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, UUID> {

    @EntityGraph(attributePaths = {"user"})
    List<LoginHistory> findByUserIdOrderByLoginTimeDesc(UUID userId);

    @EntityGraph(attributePaths = {"user"})
    Optional<LoginHistory> findTopByUserIdOrderByLoginTimeDesc(UUID userId);

    @EntityGraph(attributePaths = {"user"})
    Page<LoginHistory> findAllByOrderByLoginTimeDesc(Pageable pageable);
}
