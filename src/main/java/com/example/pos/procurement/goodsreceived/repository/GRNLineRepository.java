package com.example.pos.procurement.goodsreceived.repository;

import com.example.pos.procurement.goodsreceived.model.GRNLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GRNLineRepository extends JpaRepository<GRNLine, UUID> {
}
