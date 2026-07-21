package com.example.pos.sync.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TerminalRepository extends JpaRepository<Terminal, String> {

    Optional<Terminal> findByApiKey(String apiKey);

    Optional<Terminal> findByName(String name);

    List<Terminal> findByActive(boolean active);

    List<Terminal> findBySynced(boolean synced);

    boolean existsByName(String name);
}
