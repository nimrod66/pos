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
import com.example.pos.terminal.repository.TerminalConfigurationRepository;
import com.example.pos.terminal.repository.TerminalRegistryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TerminalRegistrationService {

    private final TerminalRegistryRepository terminalRepository;
    private final HardwarePeripheralRepository hardwarePeripheralRepository;
    private final TerminalConfigurationRepository configurationRepository;
    private final BranchRepository branchRepository;
    private final com.example.pos.user.users.repository.UserRepository userRepository;
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

    /**
     * Generates a short one-time pairing code so the terminal can be
     * activated from ANOTHER device (e.g. the cashier's PC): that device
     * signs in, opens the POS, and enters this code.
     */
    public java.util.Map<String, Object> startPairing(String terminalId) {
        Terminal terminal = scopedTerminal(terminalId);
        if (terminal.getStatus() != TerminalStatus.ACTIVE
                && terminal.getStatus() != TerminalStatus.PENDING) {
            throw new BadRequestException(
                    "Only pending or active terminals can issue a pairing code",
                    "TERMINAL_NOT_PAIRABLE");
        }
        LocalDateTime expires = LocalDateTime.now().plusMinutes(15);
        String code = Terminal.generatePairingCode();
        terminal.setPairingCode(code);
        terminal.setPairingExpiresAt(expires);
        terminalRepository.save(terminal);
        return java.util.Map.of(
                "terminalId", terminal.getTerminalId(),
                "code", code,
                "expiresAt", expires.toString());
    }

    /** Activates this device against a pairing code (any signed-in staff). */
    @Transactional(readOnly = true)
    public TerminalResponseDto pairByCode(String code) {
        String normalized = code == null ? "" : code.trim();
        Terminal terminal = terminalRepository.findAll().stream()
                .filter(item -> normalized.equals(item.getPairingCode()))
                .filter(item -> item.getPairingExpiresAt() != null
                        && item.getPairingExpiresAt().isAfter(LocalDateTime.now()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "This pairing code is invalid or has expired. Generate a new one.",
                        "INVALID_PAIRING_CODE"));
        if (!current.branchId().equals(terminal.getBranchId())) {
            throw new ForbiddenException(
                    "This pairing code belongs to another branch");
        }
        terminal.setPairingCode(null);
        terminal.setPairingExpiresAt(null);
        terminalRepository.save(terminal);
        return toDto(terminal);
    }

    /** Assigns (or clears) the staff member responsible for a terminal. */
    public TerminalResponseDto assignUser(String terminalId, UUID userId) {
        Terminal terminal = scopedTerminal(terminalId);
        if (userId != null) {
            com.example.pos.user.users.model.User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));
            if (user.getBranch() == null || user.getBranch().getPharmacy() == null
                    || !current.pharmacyId().equals(user.getBranch().getPharmacy().getId())) {
                throw new ForbiddenException("That user belongs to a different pharmacy");
            }
        }
        terminal.setAssignedUserId(userId);
        terminal = terminalRepository.save(terminal);
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

    @Transactional(readOnly = true)
    public CashRegisterConfigDto getCashRegisterConfig(String terminalId) {
        Terminal terminal = scopedTerminal(terminalId);
        Map<String, String> values = configurationRepository.findByTerminalId(terminal.getId())
                .stream()
                .collect(Collectors.toMap(
                        TerminalConfiguration::getConfigKey,
                        config -> config.getConfigValue() == null ? "" : config.getConfigValue(),
                        (left, right) -> right));
        return CashRegisterConfigDto.builder()
                .defaultOpeningFloat(decimal(values, "register.defaultOpeningFloat", "0.00"))
                .cashEnabled(bool(values, "register.cashEnabled", true))
                .mpesaEnabled(bool(values, "register.mpesaEnabled", true))
                .requireOpenShift(bool(values, "register.requireOpenShift", true))
                .autoPrintReceipt(bool(values, "register.autoPrintReceipt", true))
                .openDrawerOnCashSale(bool(values, "register.openDrawerOnCashSale", true))
                .receiptCopies(integer(values, "register.receiptCopies", 1))
                .receiptPaperWidth(integer(values, "register.receiptPaperWidth", 80))
                .scannerMode(values.getOrDefault("register.scannerMode", "KEYBOARD_WEDGE"))
                .barcodeSubmitKey(values.getOrDefault("register.barcodeSubmitKey", "ENTER"))
                .build();
    }

    public CashRegisterConfigDto updateCashRegisterConfig(
            String terminalId, CashRegisterConfigRequestDto request) {
        Terminal terminal = scopedTerminal(terminalId);
        if (request.getReceiptPaperWidth() != 58 && request.getReceiptPaperWidth() != 80) {
            throw new BadRequestException("Receipt paper width must be 58 mm or 80 mm",
                    "INVALID_RECEIPT_WIDTH");
        }
        String scannerMode = request.getScannerMode().trim().toUpperCase(Locale.ROOT);
        if (!List.of("KEYBOARD_WEDGE", "CAMERA", "LOCAL_CONNECTOR").contains(scannerMode)) {
            throw new BadRequestException("Unsupported scanner mode", "INVALID_SCANNER_MODE");
        }
        String submitKey = request.getBarcodeSubmitKey().trim().toUpperCase(Locale.ROOT);
        if (!List.of("ENTER", "TAB").contains(submitKey)) {
            throw new BadRequestException("Barcode submit key must be ENTER or TAB",
                    "INVALID_BARCODE_SUBMIT_KEY");
        }

        upsertConfig(terminal, "register.defaultOpeningFloat",
                request.getDefaultOpeningFloat().toPlainString(), "Default shift opening float");
        upsertConfig(terminal, "register.cashEnabled",
                request.getCashEnabled().toString(), "Allow cash checkout");
        upsertConfig(terminal, "register.mpesaEnabled",
                request.getMpesaEnabled().toString(), "Allow M-Pesa checkout");
        upsertConfig(terminal, "register.requireOpenShift",
                request.getRequireOpenShift().toString(), "Require an active shift for checkout");
        upsertConfig(terminal, "register.autoPrintReceipt",
                request.getAutoPrintReceipt().toString(), "Print receipt after successful checkout");
        upsertConfig(terminal, "register.openDrawerOnCashSale",
                request.getOpenDrawerOnCashSale().toString(), "Open drawer after a cash sale");
        upsertConfig(terminal, "register.receiptCopies",
                request.getReceiptCopies().toString(), "Receipt copies per sale");
        upsertConfig(terminal, "register.receiptPaperWidth",
                request.getReceiptPaperWidth().toString(), "Receipt paper width in millimetres");
        upsertConfig(terminal, "register.scannerMode", scannerMode, "Barcode scanner mode");
        upsertConfig(terminal, "register.barcodeSubmitKey", submitKey, "Scanner suffix key");
        return getCashRegisterConfig(terminalId);
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
                .assignedUserId(terminal.getAssignedUserId())
                .assignedUserName(resolveUserName(terminal.getAssignedUserId()))
                .peripherals(peripherals)
                .createdAt(terminal.getCreatedAt())
                .updatedAt(terminal.getUpdatedAt())
                .build();
    }

    private String resolveUserName(UUID userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .map(user -> user.getFirstName() + " " + user.getLastName())
                .orElse(null);
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

    private void upsertConfig(Terminal terminal, String key, String value, String description) {
        TerminalConfiguration config = configurationRepository
                .findByTerminalIdAndConfigKey(terminal.getId(), key)
                .orElseGet(() -> TerminalConfiguration.builder()
                        .terminal(terminal)
                        .configKey(key)
                        .description(description)
                        .build());
        config.setConfigValue(value);
        config.setDescription(description);
        configurationRepository.save(config);
    }

    private boolean bool(Map<String, String> values, String key, boolean fallback) {
        String value = values.get(key);
        return value == null || value.isBlank() ? fallback : Boolean.parseBoolean(value);
    }

    private int integer(Map<String, String> values, String key, int fallback) {
        try {
            return Integer.parseInt(values.getOrDefault(key, String.valueOf(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private BigDecimal decimal(Map<String, String> values, String key, String fallback) {
        try {
            return new BigDecimal(values.getOrDefault(key, fallback));
        } catch (NumberFormatException ignored) {
            return new BigDecimal(fallback);
        }
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
