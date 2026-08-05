package com.example.pos.insurance.repository;

import java.util.UUID;

import com.example.pos.insurance.model.Insurer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsurerRepository extends JpaRepository<Insurer, UUID> {

    Optional<Insurer> findByCode(String code);

    List<Insurer> findByInsurerType(Insurer.InsurerType type);

    List<Insurer> findByStatus(Insurer.Status status);

    boolean existsByCode(String code);

    boolean existsByName(String name);
}
