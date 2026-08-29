package com.example.pos.pharmacy.regulatory.interactions.repository;

import com.example.pos.pharmacy.regulatory.interactions.model.DrugInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DrugInteractionRepository extends JpaRepository<DrugInteraction, UUID> {

    @Query("""
        SELECT di FROM DrugInteraction di
        WHERE di.active = true
          AND (
            (di.medicine1.id = :m1 AND di.medicine2.id = :m2)
            OR (di.medicine1.id = :m2 AND di.medicine2.id = :m1)
          )
        """)
    List<DrugInteraction> findBetween(@Param("m1") UUID medicine1Id, @Param("m2") UUID medicine2Id);

    @Query("""
        SELECT di FROM DrugInteraction di
        WHERE di.active = true
          AND (di.medicine1.id IN :ids OR di.medicine2.id IN :ids)
        """)
    List<DrugInteraction> findAllForMedicines(@Param("ids") List<UUID> medicineIds);
}
