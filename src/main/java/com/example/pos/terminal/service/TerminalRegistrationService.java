package com.example.pos.terminal.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.terminal.dto.*;
import com.example.pos.terminal.model.*;
import com.example.pos.terminal.repository.HardwarePeripheralRepository;
import com.example.pos.terminal.repository.TerminalRegistryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TerminalRegistrationService {

    private final TerminalRegistryRepository terminalRepository;
    private final HardwarePeripheralRepository hardwarePeripheralRepository;

    public TerminalResponseDto registerTerminal(TerminalRegisterRequestDto request) {
        if (terminalRepository.existsByName(request.getName())) {
            throw new BadRequestException("Terminal with name '" + request.getName() + "' already exists");
        }

        String terminalId = generateTerminalId();

        Terminal terminal = Terminal.builder()
                .terminalId(terminalId)
                .name(request.getName())
                .terminalType(request.getTerminalType())
                .manufacturer(request.getManufacturer())
                .model(request.getModel())
                .serialNumber(request.getSerialNumber())
                .platform(request.getPlatform())
                .osVersion(request.getOsVersion())
                .firmwareVersion(request.getFirmwareVersion())
                .apiKey(Terminal.generateApiKey())
                .apiSecret(Terminal.generateApiSecret())
                .status(TerminalStatus.PENDING)
                .branchId(request.getBranchId())
                .registeredAt(LocalDateTime.now())
                .build();

        terminal = terminalRepository.save(terminal);
        log.info("Terminal registered: {} ({})", terminal.getName(), terminal.getTerminalId());
        return toDto(terminal);
    }

    @Transactional(readOnly = true)
    public TerminalResponseDto getById(UUID id) {
        Terminal terminal = terminalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Terminal not found: " + id));
        return toDto(terminal);
    }

    @Transactional(readOnly = true)
    public TerminalResponseDto getByTerminalId(String terminalId) {
        Terminal terminal = terminalRepository.findByTerminalId(terminalId)
                .orElseThrow(() -> new ResourceNotFoundException("Terminal not found: " + terminalId));
        return toDto(terminal);
    }

    @Transactional(readOnly = true)
    public Terminal getTerminalEntity(String terminalId) {
        return terminalRepository.findByTerminalId(terminalId)
                .orElseThrow(() -> new ResourceNotFoundException("Terminal not found: " + terminalId));
    }

    @Transactional(readOnly = true)
    public List<TerminalResponseDto> listAll() {
        return terminalRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TerminalResponseDto> listByStatus(TerminalStatus status) {
        return terminalRepository.findByStatus(status).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TerminalResponseDto> listByBranch(UUID branchId) {
        return terminalRepository.findByBranchId(branchId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public TerminalResponseDto approve(String terminalId) {
        Terminal terminal = terminalRepository.findByTerminalId(terminalId)
                .orElseThrow(() -> new ResourceNotFoundException("Terminal not found: " + terminalId));

        if (terminal.getStatus() != TerminalStatus.PENDING) {
            throw new BadRequestException("Terminal is not in PENDING status");
        }

        terminal.setStatus(TerminalStatus.ACTIVE);
        terminal.setLastUpdate(LocalDateTime.now());
        terminal = terminalRepository.save(terminal);
        log.info("Terminal approved: {}", terminal.getTerminalId());
        return toDto(terminal);
    }

    public TerminalResponseDto deactivate(String terminalId) {
        Terminal terminal = terminalRepository.findByTerminalId(terminalId)
                .orElseThrow(() -> new ResourceNotFoundException("Terminal not found: " + terminalId));

        if (terminal.getStatus() != TerminalStatus.ACTIVE) {
            throw new BadRequestException("Terminal is not in ACTIVE status");
        }

        terminal.setStatus(TerminalStatus.DEACTIVATED);
        terminal.setLastUpdate(LocalDateTime.now());
        terminal = terminalRepository.save(terminal);
        log.info("Terminal deactivated: {}", terminal.getTerminalId());
        return toDto(terminal);
    }

    public TerminalResponseDto block(String terminalId) {
        Terminal terminal = terminalRepository.findByTerminalId(terminalId)
                .orElseThrow(() -> new ResourceNotFoundException("Terminal not found: " + terminalId));

        terminal.setStatus(TerminalStatus.BLOCKED);
        terminal.setLastUpdate(LocalDateTime.now());
        terminal = terminalRepository.save(terminal);
        log.warn("Terminal blocked: {}", terminal.getTerminalId());
        return toDto(terminal);
    }

    public TerminalResponseDto regenerateKey(String terminalId) {
        Terminal terminal = terminalRepository.findByTerminalId(terminalId)
                .orElseThrow(() -> new ResourceNotFoundException("Terminal not found: " + terminalId));

        terminal.setApiKey(Terminal.generateApiKey());
        terminal.setApiSecret(Terminal.generateApiSecret());
        terminal.setLastUpdate(LocalDateTime.now());
        terminal = terminalRepository.save(terminal);
        log.info("API key regenerated for terminal: {}", terminal.getTerminalId());
        return toDto(terminal);
    }

    public TerminalResponseDto updateMetadata(String terminalId, TerminalRegisterRequestDto request) {
        Terminal terminal = terminalRepository.findByTerminalId(terminalId)
                .orElseThrow(() -> new ResourceNotFoundException("Terminal not found: " + terminalId));

        terminal.setName(request.getName());
        terminal.setTerminalType(request.getTerminalType());
        terminal.setManufacturer(request.getManufacturer());
        terminal.setModel(request.getModel());
        terminal.setSerialNumber(request.getSerialNumber());
        terminal.setPlatform(request.getPlatform());
        terminal.setOsVersion(request.getOsVersion());
        terminal.setFirmwareVersion(request.getFirmwareVersion());
        terminal.setBranchId(request.getBranchId());
        terminal.setLastUpdate(LocalDateTime.now());
        terminal = terminalRepository.save(terminal);
        return toDto(terminal);
    }

    public void updateLastSeen(String terminalId) {
        terminalRepository.findByTerminalId(terminalId).ifPresent(terminal -> {
            terminal.setLastSeenAt(LocalDateTime.now());
            terminalRepository.save(terminal);
        });
    }

    @Transactional(readOnly = true)
    public List<HardwarePeripheralDto> getPeripherals(String terminalId) {
        Terminal terminal = terminalRepository.findByTerminalId(terminalId)
                .orElseThrow(() -> new ResourceNotFoundException("Terminal not found: " + terminalId));
        return hardwarePeripheralRepository.findByTerminalId(terminal.getId()).stream()
                .map(this::toPeripheralDto)
                .collect(Collectors.toList());
    }

    public HardwarePeripheralDto addPeripheral(String terminalId, HardwarePeripheralRequestDto request) {
        Terminal terminal = terminalRepository.findByTerminalId(terminalId)
                .orElseThrow(() -> new ResourceNotFoundException("Terminal not found: " + terminalId));

        HardwarePeripheral peripheral = HardwarePeripheral.builder()
                .terminal(terminal)
                .type(request.getType())
                .manufacturer(request.getManufacturer())
                .model(request.getModel())
                .connectionType(request.getConnectionType())
                .configuration(request.getConfiguration())
                .status(PeripheralStatus.UNKNOWN)
                .build();

        peripheral = hardwarePeripheralRepository.save(peripheral);
        return toPeripheralDto(peripheral);
    }

    public void removePeripheral(UUID peripheralId) {
        hardwarePeripheralRepository.deleteById(peripheralId);
    }

    public HardwarePeripheralDto updatePeripheralStatus(UUID peripheralId, PeripheralStatus status) {
        HardwarePeripheral peripheral = hardwarePeripheralRepository.findById(peripheralId)
                .orElseThrow(() -> new ResourceNotFoundException("Hardware peripheral not found: " + peripheralId));
        peripheral.setStatus(status);
        peripheral = hardwarePeripheralRepository.save(peripheral);
        return toPeripheralDto(peripheral);
    }

    public List<HardwarePeripheralDto> replacePeripherals(String terminalId, List<HardwarePeripheralRequestDto> requests) {
        Terminal terminal = terminalRepository.findByTerminalId(terminalId)
                .orElseThrow(() -> new ResourceNotFoundException("Terminal not found: " + terminalId));

        hardwarePeripheralRepository.deleteAllByTerminalId(terminal.getId());

        List<HardwarePeripheral> peripherals = requests.stream()
                .map(req -> HardwarePeripheral.builder()
                        .terminal(terminal)
                        .type(req.getType())
                        .manufacturer(req.getManufacturer())
                        .model(req.getModel())
                        .connectionType(req.getConnectionType())
                        .configuration(req.getConfiguration())
                        .status(PeripheralStatus.UNKNOWN)
                        .build())
                .collect(Collectors.toList());

        return hardwarePeripheralRepository.saveAll(peripherals).stream()
                .map(this::toPeripheralDto)
                .collect(Collectors.toList());
    }

    private TerminalResponseDto toDto(Terminal terminal) {
        List<HardwarePeripheralDto> peripherals = hardwarePeripheralRepository
                .findByTerminalId(terminal.getId()).stream()
                .map(this::toPeripheralDto)
                .collect(Collectors.toList());

        return TerminalResponseDto.builder()
                .id(terminal.getId())
                .terminalId(terminal.getTerminalId())
                .name(terminal.getName())
                .terminalType(terminal.getTerminalType())
                .manufacturer(terminal.getManufacturer())
                .model(terminal.getModel())
                .serialNumber(terminal.getSerialNumber())
                .platform(terminal.getPlatform())
                .osVersion(terminal.getOsVersion())
                .firmwareVersion(terminal.getFirmwareVersion())
                .status(terminal.getStatus())
                .branchId(terminal.getBranchId())
                .registeredBy(terminal.getRegisteredBy())
                .registeredAt(terminal.getRegisteredAt())
                .lastSeenAt(terminal.getLastSeenAt())
                .appVersion(terminal.getAppVersion())
                .supportedApiVersion(terminal.getSupportedApiVersion())
                .lastUpdate(terminal.getLastUpdate())
                .minimumBackendVersion(terminal.getMinimumBackendVersion())
                .migratedFromTerminal(terminal.isMigratedFromTerminal())
                .peripherals(peripherals)
                .createdAt(terminal.getCreatedAt())
                .updatedAt(terminal.getUpdatedAt())
                .build();
    }

    private HardwarePeripheralDto toPeripheralDto(HardwarePeripheral p) {
        return HardwarePeripheralDto.builder()
                .id(p.getId())
                .type(p.getType())
                .manufacturer(p.getManufacturer())
                .model(p.getModel())
                .connectionType(p.getConnectionType())
                .status(p.getStatus())
                .configuration(p.getConfiguration())
                .build();
    }

    private String generateTerminalId() {
        return "T-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
