package com.example.pos.terminal.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ForbiddenException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.branch.repository.BranchRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
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
    private final BranchRepository branchRepository;
    private final AuthenticatedUserContext current;

    public TerminalResponseDto registerTerminal(TerminalRegisterRequestDto request) {
        Branch branch = accessibleBranch(request.getBranchId());
        String name = request.getName().trim();
        if (terminalRepository.existsByBranchIdAndNameIgnoreCase(branch.getId(), name)) {
            throw new BadRequestException("Terminal with name '" + request.getName() + "' already exists");
        }

        String terminalId = generateTerminalId();

        Terminal terminal = Terminal.builder()
                .terminalId(terminalId)
                .name(name)
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
                .branchId(branch.getId())
                .registeredBy(current.user().getEmail())
                .registeredAt(LocalDateTime.now())
                .build();

        terminal = terminalRepository.save(terminal);
        log.info("Terminal registered: {} ({})", terminal.getName(), terminal.getTerminalId());
        return toDto(terminal);
    }

    @Transactional(readOnly = true)
    public TerminalResponseDto getById(UUID id) {
        return toDto(scopedTerminal(id));
    }

    @Transactional(readOnly = true)
    public TerminalResponseDto getByTerminalId(String terminalId) {
        return toDto(scopedTerminal(terminalId));
    }

    @Transactional(readOnly = true)
    public Terminal getTerminalEntity(String terminalId) {
        return scopedTerminal(terminalId);
    }

    @Transactional(readOnly = true)
    public List<TerminalResponseDto> listAll() {
        return terminalRepository.findByBranchIdIn(visibleBranchIds()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TerminalResponseDto> listByStatus(TerminalStatus status) {
        return terminalRepository.findByBranchIdInAndStatus(visibleBranchIds(), status).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TerminalResponseDto> listByBranch(UUID branchId) {
        accessibleBranch(branchId);
        return terminalRepository.findByBranchId(branchId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public TerminalResponseDto approve(String terminalId) {
        Terminal terminal = scopedTerminal(terminalId);

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
        Terminal terminal = scopedTerminal(terminalId);

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
        Terminal terminal = scopedTerminal(terminalId);

        terminal.setStatus(TerminalStatus.BLOCKED);
        terminal.setLastUpdate(LocalDateTime.now());
        terminal = terminalRepository.save(terminal);
        log.warn("Terminal blocked: {}", terminal.getTerminalId());
        return toDto(terminal);
    }

    public TerminalResponseDto regenerateKey(String terminalId) {
        Terminal terminal = scopedTerminal(terminalId);

        terminal.setApiKey(Terminal.generateApiKey());
        terminal.setApiSecret(Terminal.generateApiSecret());
        terminal.setLastUpdate(LocalDateTime.now());
        terminal = terminalRepository.save(terminal);
        log.info("API key regenerated for terminal: {}", terminal.getTerminalId());
        return toDto(terminal);
    }

    public TerminalResponseDto updateMetadata(String terminalId, TerminalRegisterRequestDto request) {
        Terminal terminal = scopedTerminal(terminalId);
        Branch branch = accessibleBranch(request.getBranchId());
        String name = request.getName().trim();
        if (terminalRepository.existsByBranchIdAndNameIgnoreCaseAndIdNot(
                branch.getId(), name, terminal.getId())) {
            throw new BadRequestException("Terminal with name '" + request.getName() + "' already exists");
        }

        terminal.setName(name);
        terminal.setTerminalType(request.getTerminalType());
        terminal.setManufacturer(request.getManufacturer());
        terminal.setModel(request.getModel());
        terminal.setSerialNumber(request.getSerialNumber());
        terminal.setPlatform(request.getPlatform());
        terminal.setOsVersion(request.getOsVersion());
        terminal.setFirmwareVersion(request.getFirmwareVersion());
        terminal.setBranchId(branch.getId());
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
        Terminal terminal = scopedTerminal(terminalId);
        return hardwarePeripheralRepository.findByTerminalId(terminal.getId()).stream()
                .map(this::toPeripheralDto)
                .collect(Collectors.toList());
    }

    public HardwarePeripheralDto addPeripheral(String terminalId, HardwarePeripheralRequestDto request) {
        Terminal terminal = scopedTerminal(terminalId);

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
        HardwarePeripheral peripheral = scopedPeripheral(peripheralId);
        hardwarePeripheralRepository.delete(peripheral);
    }

    public HardwarePeripheralDto updatePeripheralStatus(UUID peripheralId, PeripheralStatus status) {
        HardwarePeripheral peripheral = scopedPeripheral(peripheralId);
        peripheral.setStatus(status);
        peripheral = hardwarePeripheralRepository.save(peripheral);
        return toPeripheralDto(peripheral);
    }

    public List<HardwarePeripheralDto> replacePeripherals(String terminalId, List<HardwarePeripheralRequestDto> requests) {
        Terminal terminal = scopedTerminal(terminalId);

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
                .branchName(branchRepository.findById(terminal.getBranchId())
                        .map(Branch::getBranchName).orElse(null))
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

    private Terminal scopedTerminal(UUID id) {
        Terminal terminal = terminalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Terminal not found: " + id));
        requireVisibleBranch(terminal.getBranchId());
        return terminal;
    }

    private Terminal scopedTerminal(String terminalId) {
        Terminal terminal = terminalRepository.findByTerminalId(terminalId)
                .orElseThrow(() -> new ResourceNotFoundException("Terminal not found: " + terminalId));
        requireVisibleBranch(terminal.getBranchId());
        return terminal;
    }

    private HardwarePeripheral scopedPeripheral(UUID peripheralId) {
        HardwarePeripheral peripheral = hardwarePeripheralRepository.findById(peripheralId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Hardware peripheral not found: " + peripheralId));
        requireVisibleBranch(peripheral.getTerminal().getBranchId());
        return peripheral;
    }

    private Branch accessibleBranch(UUID branchId) {
        Branch branch = branchRepository.findByIdAndPharmacyId(branchId, current.pharmacyId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
        if (!current.hasAuthority("ROLE_OWNER") && !current.branchId().equals(branchId)) {
            throw new ForbiddenException("The terminal branch is outside the active session");
        }
        return branch;
    }

    private List<UUID> visibleBranchIds() {
        if (current.hasAuthority("ROLE_OWNER")) {
            return branchRepository.findByPharmacyId(current.pharmacyId()).stream()
                    .map(Branch::getId)
                    .toList();
        }
        return List.of(current.branchId());
    }

    private void requireVisibleBranch(UUID branchId) {
        if (branchId == null || !visibleBranchIds().contains(branchId)) {
            throw new ResourceNotFoundException("Terminal");
        }
    }
}
