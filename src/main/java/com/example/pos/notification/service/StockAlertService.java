package com.example.pos.notification.service;

import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.branch.repository.BranchRepository;
import com.example.pos.core.pharmacy.model.Pharmacy;
import com.example.pos.core.pharmacy.repository.PharmacyRepository;
import com.example.pos.core.systemsettings.model.SystemSettings;
import com.example.pos.core.systemsettings.repository.SystemSettingsRepository;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.inventory.stock.model.Stock;
import com.example.pos.inventory.stock.repository.StockRepository;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.masterdata.medicine.repository.MedicineRepository;
import com.example.pos.notification.email.EmailService;
import com.example.pos.notification.model.Notification;
import com.example.pos.notification.repository.NotificationRepository;
import com.example.pos.user.users.model.User;
import com.example.pos.user.users.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Scheduled scanner that turns stock state into branch notifications:
 * LOW_STOCK when sellable quantity falls to the reorder level, and
 * EXPIRY_WARNING inside each branch's configured expiry alert window.
 * Unread duplicates of the same type + reference are never recreated.
 * Optionally sends email alerts to branch managers when SMTP is configured.
 */
@Slf4j
@Service
public class StockAlertService {

    private static final String LOW_STOCK_KEY = "inventory.low_stock_threshold";
    private static final String EXPIRY_DAYS_KEY = "inventory.expiry_alert_days";

    private final PharmacyRepository pharmacyRepository;
    private final BranchRepository branchRepository;
    private final StockRepository stockRepository;
    private final MedicineRepository medicineRepository;
    private final SystemSettingsRepository settingsRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public StockAlertService(PharmacyRepository pharmacyRepository,
                             BranchRepository branchRepository,
                             StockRepository stockRepository,
                             MedicineRepository medicineRepository,
                             SystemSettingsRepository settingsRepository,
                             NotificationRepository notificationRepository,
                             UserRepository userRepository,
                             EmailService emailService) {
        this.pharmacyRepository = pharmacyRepository;
        this.branchRepository = branchRepository;
        this.stockRepository = stockRepository;
        this.medicineRepository = medicineRepository;
        this.settingsRepository = settingsRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Scheduled(fixedDelay = 900_000, initialDelay = 60_000)
    public void scanStockLevels() {
        for (Pharmacy pharmacy : pharmacyRepository.findAll()) {
            try {
                scanPharmacy(pharmacy);
            } catch (Exception ex) {
                log.error("Stock alert scan failed for pharmacy {}: {}",
                        pharmacy.getId(), ex.getMessage());
            }
        }
    }

    private void scanPharmacy(Pharmacy pharmacy) {
        List<Branch> branches = branchRepository.findByPharmacyId(pharmacy.getId()).stream()
                .filter(branch -> branch.getStatus() == Branch.Status.ACTIVE)
                .toList();
        if (branches.isEmpty()) {
            return;
        }

        Map<UUID, Medicine> medicines = new HashMap<>();
        for (Medicine medicine : medicineRepository.findAllByPharmacyId(pharmacy.getId())) {
            medicines.put(medicine.getId(), medicine);
        }

        LocalDate today = LocalDate.now();
        for (Branch branch : branches) {
            int lowStockThreshold = intSetting(
                    LOW_STOCK_KEY, branch, pharmacy.getId(), 10);
            int expiryWindowDays = intSetting(
                    EXPIRY_DAYS_KEY, branch, pharmacy.getId(), 90);
            List<Stock> stocks = stockRepository.findByBranchIdIn(List.of(branch.getId()));

            Map<UUID, Integer> sellableByMedicine = new HashMap<>();
            for (Stock stock : stocks) {
                scanStockRow(branch, today, expiryWindowDays, sellableByMedicine, stock);
            }

            createLowStockNotifications(
                    branch, lowStockThreshold, sellableByMedicine, medicines);
        }
    }

    private void scanStockRow(Branch branch, LocalDate today, int expiryWindowDays,
                              Map<UUID, Integer> sellableByMedicine, Stock stock) {
        int quantity = stock.getQuantityAvailable() == null ? 0 : stock.getQuantityAvailable();
        MedicineBatches batch = stock.getMedicineBatches();
        if (quantity <= 0 || batch == null || batch.getMedicine() == null) {
            return;
        }
        LocalDate expiryDate = batch.getExpirationDate();
        boolean sellable = expiryDate == null || expiryDate.isAfter(today);
        if (sellable && batch.getMedicine().getStatus() == Medicine.Status.AVAILABLE) {
            sellableByMedicine.merge(batch.getMedicine().getId(), quantity, Integer::sum);
        }
        boolean expiringSoon = expiryDate != null
                && !expiryDate.isBefore(today)
                && !expiryDate.isAfter(today.plusDays(expiryWindowDays));
        if (expiringSoon) {
            createExpiryNotification(branch, batch, quantity);
        }
    }

    private void createLowStockNotifications(Branch branch, int fallbackThreshold,
                                             Map<UUID, Integer> sellableByMedicine,
                                             Map<UUID, Medicine> medicines) {
        for (Map.Entry<UUID, Integer> entry : sellableByMedicine.entrySet()) {
            Medicine medicine = medicines.get(entry.getKey());
            if (medicine == null || medicine.getStatus() != Medicine.Status.AVAILABLE) {
                continue;
            }
            int reorderLevel = medicine.getReorderLevel() == null
                    ? fallbackThreshold : medicine.getReorderLevel();
            int available = entry.getValue();
            if (available > reorderLevel) {
                continue;
            }
            if (notificationRepository.existsByBranchIdAndTypeAndReferenceIdAndStatus(
                    branch.getId(), Notification.Type.LOW_STOCK,
                    medicine.getId(), Notification.Status.UNREAD)) {
                continue;
            }
            notificationRepository.save(Notification.builder()
                    .title("Low stock: " + safeName(medicine))
                    .message(String.format(
                            "%s (%s) is down to %d units at %s (reorder level %d).",
                            safeName(medicine), safeSku(medicine), available,
                            branch.getBranchName(), reorderLevel))
                    .type(Notification.Type.LOW_STOCK)
                    .status(Notification.Status.UNREAD)
                    .branchId(branch.getId())
                    .referenceId(medicine.getId())
                    .referenceType("MEDICINE")
                    .build());
            sendEmailAlert(branch, "Low Stock Alert",
                    String.format("%s is down to %d units (reorder level %d) at %s.",
                            safeName(medicine), available, reorderLevel, branch.getBranchName()));
        }
    }

    private void createExpiryNotification(Branch branch, MedicineBatches batch, int quantity) {
        if (notificationRepository.existsByBranchIdAndTypeAndReferenceIdAndStatus(
                branch.getId(), Notification.Type.EXPIRY_WARNING,
                batch.getId(), Notification.Status.UNREAD)) {
            return;
        }
        Medicine medicine = batch.getMedicine();
        notificationRepository.save(Notification.builder()
                .title("Batch expiring soon")
                .message(String.format(
                        "Batch %s of %s expires on %s with %d unit(s) remaining at %s.",
                        batch.getBatchNumber(), safeName(medicine),
                        batch.getExpirationDate(), quantity, branch.getBranchName()))
                .type(Notification.Type.EXPIRY_WARNING)
                .status(Notification.Status.UNREAD)
                .branchId(branch.getId())
                .referenceId(batch.getId())
                .referenceType("BATCH")
                .build());
        sendEmailAlert(branch, "Expiry Warning",
                String.format("Batch %s of %s expires on %s (%d units) at %s.",
                        batch.getBatchNumber(), safeName(medicine),
                        batch.getExpirationDate(), quantity, branch.getBranchName()));
    }

    private void sendEmailAlert(Branch branch, String subject, String body) {
        try {
            List<User> branchUsers = userRepository.findByBranchId(branch.getId());
            for (User user : branchUsers) {
                boolean isManager = user.getUserBranchRole() != null &&
                        user.getUserBranchRole().stream()
                                .anyMatch(ubr -> ubr.getRole() != null &&
                                        (ubr.getRole().getRoleName().equals("BRANCH_MANAGER") ||
                                         ubr.getRole().getRoleName().equals("OWNER")));
                if (isManager && user.getEmail() != null && !user.getEmail().isBlank()) {
                    emailService.sendStockAlert(user.getEmail(), user.getFirstName(), subject, body);
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to send stock alert email for branch {}: {}", branch.getId(), ex.getMessage());
        }
    }

    private int intSetting(String key, Branch branch, UUID pharmacyId, int fallback) {
        return settingsRepository.findSetting(key, branch.getId(), pharmacyId)
                .or(() -> settingsRepository.findSetting(key, null, pharmacyId))
                .map(SystemSettings::getSettingValue)
                .map(value -> {
                    try {
                        return Math.max(1, Integer.parseInt(value.trim()));
                    } catch (NumberFormatException ex) {
                        return fallback;
                    }
                })
                .orElse(fallback);
    }

    private String safeName(Medicine medicine) {
        return medicine.getBrandName() == null ? "Unnamed medicine" : medicine.getBrandName();
    }

    private String safeSku(Medicine medicine) {
        return medicine.getSku() == null ? "-" : medicine.getSku();
    }
}
