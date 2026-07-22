package com.example.pos.compliance.reference.repository;

import com.example.pos.compliance.reference.model.ItemClassification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ItemClassificationRepository extends JpaRepository<ItemClassification, Long> {
    Optional<ItemClassification> findByClassificationCode(String code);
}