package com.example.pos.core.systemsettings;

import com.example.pos.core.branch.repository.BranchRepository;
import com.example.pos.core.pharmacy.model.Pharmacy;
import com.example.pos.core.pharmacy.repository.PharmacyRepository;
import com.example.pos.core.systemsettings.model.SystemSettings;
import com.example.pos.core.systemsettings.repository.SystemSettingsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@Order(3)
public class SettingsSeeder implements CommandLineRunner {

    private final SystemSettingsRepository settingsRepository;
    private final PharmacyRepository pharmacyRepository;
    private final BranchRepository branchRepository;

    public SettingsSeeder(SystemSettingsRepository settingsRepository,
                          PharmacyRepository pharmacyRepository,
                          BranchRepository branchRepository) {
        this.settingsRepository = settingsRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.branchRepository = branchRepository;
    }

    @Override
    public void run(String... args) {
        List<Pharmacy> pharmacies = pharmacyRepository.findAll();
        if (pharmacies.isEmpty()) {
            log.info("No pharmacies found; skipping settings seed");
            return;
        }

        for (Pharmacy pharmacy : pharmacies) {
            if (settingsRepository.findByPharmacyId(pharmacy.getId()).isEmpty()) {
                createDefaults(pharmacy.getId(), null, pharmacy.getName());
                log.info("Seeded default settings for pharmacy: {}", pharmacy.getName());
            } else {
                ensureNewDefaults(pharmacy.getId());
            }
        }
    }

    private void ensureNewDefaults(UUID pharmacyId) {
        ensure(pharmacyId, SettingKeys.Hardware.CONNECTOR_URL,
                "http://localhost:9100", "URL of the local hardware connector service");
    }

    private void createDefaults(UUID pharmacyId, UUID branchId, String pharmacyName) {
        seed(pharmacyId, branchId, SettingKeys.Receipt.STORE_NAME, pharmacyName,
                "Store name printed on receipts");
        seed(pharmacyId, branchId, SettingKeys.Receipt.HEADER_TEXT, pharmacyName,
                "Header text on receipts");
        seed(pharmacyId, branchId, SettingKeys.Receipt.FOOTER_TEXT,
                "Thank you for your purchase. Goods once sold cannot be returned.",
                "Footer text on receipts");
        seed(pharmacyId, branchId, SettingKeys.Receipt.SHOW_LOGO, "true",
                "Show pharmacy logo on receipts");
        seed(pharmacyId, branchId, SettingKeys.Receipt.SHOW_BARCODE, "true",
                "Show barcode on receipts");
        seed(pharmacyId, branchId, SettingKeys.Receipt.SHOW_TAX_BREAKDOWN, "true",
                "Show tax breakdown on receipts");
        seed(pharmacyId, branchId, SettingKeys.Receipt.RETURN_POLICY,
                "Returns accepted within 7 days with original receipt.",
                "Return policy text on receipts");
        seed(pharmacyId, branchId, SettingKeys.Receipt.THANK_YOU_MESSAGE,
                "Thank you — Karibu Tena!",
                "Thank you message on receipts");

        seed(pharmacyId, branchId, SettingKeys.Invoice.PREFIX, "INV-",
                "Invoice number prefix");
        seed(pharmacyId, branchId, SettingKeys.Invoice.STARTING_NUMBER, "1",
                "First invoice number");
        seed(pharmacyId, branchId, SettingKeys.Invoice.RESET_FREQUENCY, "DAILY",
                "Invoice number reset: DAILY, MONTHLY, YEARLY, NEVER");

        seed(pharmacyId, branchId, SettingKeys.Inventory.LOW_STOCK_THRESHOLD, "10",
                "Low stock alert threshold");
        seed(pharmacyId, branchId, SettingKeys.Inventory.EXPIRY_ALERT_DAYS, "90",
                "Days before expiry to trigger alert");
        seed(pharmacyId, branchId, SettingKeys.Inventory.AUTO_REORDER, "false",
                "Auto-generate purchase orders for low stock");
        seed(pharmacyId, branchId, SettingKeys.Inventory.DEFAULT_REORDER_LEVEL, "50",
                "Default reorder level for new medicines");

        seed(pharmacyId, branchId, SettingKeys.Sale.TAX_RATE, "16.0",
                "Default tax rate percentage");
        seed(pharmacyId, branchId, SettingKeys.Sale.CURRENCY, "KES",
                "Currency code");
        seed(pharmacyId, branchId, SettingKeys.Sale.MAX_DISCOUNT_PERCENT, "20",
                "Maximum discount percentage allowed");
        seed(pharmacyId, branchId, SettingKeys.Sale.REQUIRE_MANAGER_FOR_VOID, "true",
                "Require manager approval to void a sale");
        seed(pharmacyId, branchId, SettingKeys.Sale.ALLOW_CREDIT_SALES, "false",
                "Allow credit sales");
        seed(pharmacyId, branchId, SettingKeys.Sale.ROUNDING_MODE, "HALF_UP",
                "Rounding mode: UP, DOWN, HALF_UP");

        seed(pharmacyId, branchId, SettingKeys.Payment.M_PESA_ENABLED, "false",
                "M-Pesa payments enabled");
        seed(pharmacyId, branchId, SettingKeys.Payment.CARD_ENABLED, "false",
                "Card payments enabled");
        seed(pharmacyId, branchId, SettingKeys.Payment.CASH_ENABLED, "true",
                "Cash payments enabled");

        seed(pharmacyId, branchId, SettingKeys.Shift.AUTO_CLOSE_HOUR, "23",
                "Hour to auto-close shifts (24h format)");
        seed(pharmacyId, branchId, SettingKeys.Shift.MAX_VARIANCE_AMOUNT, "100",
                "Maximum allowed variance before approval required");
        seed(pharmacyId, branchId, SettingKeys.Shift.REQUIRE_COUNT, "true",
                "Require cash count on shift close");

        seed(pharmacyId, branchId, SettingKeys.Etims.ENABLED, "false",
                "eTIMS integration enabled");

        seed(pharmacyId, branchId, SettingKeys.Backup.AUTO_BACKUP, "true",
                "Enable automatic backups");
        seed(pharmacyId, branchId, SettingKeys.Backup.FREQUENCY_HOURS, "24",
                "Backup frequency in hours");
    }

    private void seed(UUID pharmacyId, UUID branchId, String key, String value, String description) {
        Pharmacy pharmacy = pharmacyRepository.getReferenceById(pharmacyId);
        SystemSettings settings = SystemSettings.builder()
                .settingKey(key)
                .settingValue(value)
                .description(description)
                .pharmacy(pharmacy)
                .build();
        if (branchId != null) {
            settings.setBranch(branchRepository.getReferenceById(branchId));
        }
        settingsRepository.save(settings);
    }

    private void ensure(UUID pharmacyId, String key, String value, String description) {
        if (settingsRepository.findSetting(key, null, pharmacyId).isPresent()) {
            return;
        }
        Pharmacy pharmacy = pharmacyRepository.getReferenceById(pharmacyId);
        settingsRepository.save(SystemSettings.builder()
                .settingKey(key)
                .settingValue(value)
                .description(description)
                .pharmacy(pharmacy)
                .build());
    }
}
