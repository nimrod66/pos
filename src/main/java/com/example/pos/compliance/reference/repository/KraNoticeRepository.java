package com.example.pos.compliance.reference.repository;

import java.util.UUID;

import com.example.pos.compliance.reference.model.KraNotice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KraNoticeRepository extends JpaRepository<KraNotice, UUID> {}