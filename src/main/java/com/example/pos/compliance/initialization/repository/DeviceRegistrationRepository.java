package com.example.pos.compliance.initialization.repository;

import java.util.UUID;

import com.example.pos.compliance.initialization.model.DeviceRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceRegistrationRepository extends JpaRepository<DeviceRegistration, UUID> {

    Optional<DeviceRegistration> findByDeviceSerial(String deviceSerial);

    Optional<DeviceRegistration> findByKraPinAndEnvironment(String kraPin, String environment);
}
