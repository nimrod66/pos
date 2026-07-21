package com.example.pos.compliance.etims.repository;

import com.example.pos.compliance.etims.model.Etims;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EtimsRepository extends JpaRepository<Etims, Long> {

    List<Etims> findBySalesId(Long saleId);
}
