package com.example.pos.terminal.repository;

import java.util.UUID;

import com.example.pos.terminal.model.HardwarePeripheral;
import com.example.pos.terminal.model.PeripheralType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HardwarePeripheralRepository extends JpaRepository<HardwarePeripheral, UUID> {
    List<HardwarePeripheral> findByTerminalId(UUID terminalId);
    List<HardwarePeripheral> findByTerminalIdAndType(UUID terminalId, PeripheralType type);
    void deleteAllByTerminalId(UUID terminalId);
}
