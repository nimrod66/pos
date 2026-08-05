package com.example.pos.masterdata.manufacturer.repository;

import java.util.UUID;

import com.example.pos.masterdata.manufacturer.model.Manufacturer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ManufacturerRepository extends JpaRepository<Manufacturer, UUID> {

    Optional<Manufacturer> findByManufacturerName(String name);

    boolean existsByManufacturerName(String name);

    boolean existsByManufacturerNameAndIdNot(String name, UUID id);
}
