package com.example.pos.terminal.repository;

import com.example.pos.terminal.model.HardwarePeripheral;
import com.example.pos.terminal.model.PeripheralType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HardwarePeripheralRepository extends JpaRepository<HardwarePeripheral, Long> {
    List<HardwarePeripheral> findByTerminalId(Long terminalId);
    List<HardwarePeripheral> findByTerminalIdAndType(Long terminalId, PeripheralType type);
    void deleteAllByTerminalId(Long terminalId);
}
