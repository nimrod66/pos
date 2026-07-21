package com.example.pos.sale.receipts.service;

import com.example.pos.core.systemsettings.SettingKeys;
import com.example.pos.core.systemsettings.model.SystemSettings;
import com.example.pos.core.systemsettings.repository.SystemSettingsRepository;
import com.example.pos.sale.payment.model.Payment;
import com.example.pos.sale.saleitems.model.SaleItems;
import com.example.pos.sale.sales.model.Sales;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReceiptService {

    private final SystemSettingsRepository settingsRepo;

    public ReceiptService(SystemSettingsRepository settingsRepo) {
        this.settingsRepo = settingsRepo;
    }

    public ReceiptData generate(Sales sale) {
        List<SystemSettings> settings = settingsRepo.findByPharmacyId(
                sale.getBranch().getPharmacy().getId());

        String storeName = getSetting(settings, SettingKeys.Receipt.STORE_NAME, "Pharmacy");
        String footer = getSetting(settings, SettingKeys.Receipt.FOOTER_TEXT,
                "Thank you for your purchase");
        String currency = getSetting(settings, SettingKeys.Sale.CURRENCY, "KES");

        ReceiptData data = new ReceiptData();
        data.setStoreName(storeName);
        data.setBranchName(sale.getBranch().getBranchName());
        data.setAddress(sale.getBranch().getLocation());
        data.setPhone(sale.getBranch().getPhoneNumber());
        data.setInvoiceNumber(sale.getInvoiceNumber());
        data.setDateTime(sale.getCreatedAt());
        data.setCashier(sale.getUser() != null
                ? sale.getUser().getFirstName() + " " + sale.getUser().getLastName()
                : "Unknown");
        data.setCurrency(currency);

        List<ReceiptData.ReceiptItem> items = new ArrayList<>();
        int itemNo = 1;
        for (SaleItems si : sale.getSaleItems()) {
            ReceiptData.ReceiptItem item = new ReceiptData.ReceiptItem();
            item.setNumber(itemNo++);
            item.setName(si.getMedicineBatches() != null && si.getMedicineBatches().getMedicine() != null
                    ? si.getMedicineBatches().getMedicine().getBrandName() : "-");
            item.setBatchNumber(si.getMedicineBatches() != null
                    ? si.getMedicineBatches().getBatchNumber() : "-");
            item.setQty(si.getQuantity());
            item.setUnitPrice(si.getPrice());
            item.setDiscount(si.getDiscount() != null ? si.getDiscount() : BigDecimal.ZERO);
            item.setTotal(si.getTotal());
            items.add(item);
        }
        data.setItems(items);

        data.setSubtotal(sale.getSubtotal());
        data.setTax(sale.getTax() != null ? sale.getTax() : BigDecimal.ZERO);
        data.setTotal(sale.getTotal());

        data.setPaymentMethods(sale.getPayment().stream()
                .map(p -> p.getPaymentMethod().name() + " " + p.getAmount())
                .collect(Collectors.joining(", ")));

        data.setFooterText(footer);
        data.setThankYou(getSetting(settings, SettingKeys.Receipt.THANK_YOU_MESSAGE, "Karibu Tena!"));
        data.setReturnPolicy(getSetting(settings, SettingKeys.Receipt.RETURN_POLICY,
                "Returns accepted within 7 days with original receipt."));

        return data;
    }

    public String generateEscPos(Sales sale) {
        ReceiptData data = generate(sale);
        StringBuilder sb = new StringBuilder();

        sb.append('\u001B').append('@');
        sb.append('\u001B').append('a').append('\u0001');
        sb.append(centerPad(data.getStoreName(), 42)).append('\n');
        sb.append(centerPad(data.getBranchName(), 42)).append('\n');

        if (data.getAddress() != null) {
            sb.append(centerPad(data.getAddress(), 42)).append('\n');
        }
        if (data.getPhone() != null) {
            sb.append(centerPad("Tel: " + data.getPhone(), 42)).append('\n');
        }

        sb.append(drawLine(42));
        sb.append(padColumns("Invoice:", data.getInvoiceNumber(), 42)).append('\n');
        sb.append(padColumns("Date:", formatDateTime(data.getDateTime()), 42)).append('\n');
        sb.append(padColumns("Cashier:", data.getCashier(), 42)).append('\n');
        sb.append(drawLine(42));

        sb.append(String.format("%-2s %-18s %3s %6s %8s\n",
                "#", "Item", "Qty", "Price", "Total"));
        sb.append(drawLine(42));

        for (ReceiptData.ReceiptItem item : data.getItems()) {
            String name = item.getName().length() > 18
                    ? item.getName().substring(0, 15) + ".."
                    : item.getName();
            sb.append(String.format("%-2d %-18s %3d %6s %8s\n",
                    item.getNumber(), name, item.getQty(),
                    formatMoney(item.getUnitPrice()),
                    formatMoney(item.getTotal())));

            if (item.getDiscount() != null && item.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
                sb.append(String.format("   Disc: %s\n", formatMoney(item.getDiscount().negate())));
            }
        }

        sb.append(drawLine(42));
        sb.append(padColumns("Subtotal:", formatMoney(data.getSubtotal()), 42)).append('\n');
        if (data.getTax().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(padColumns("Tax:", formatMoney(data.getTax()), 42)).append('\n');
        }
        sb.append('\u001B').append('E').append('\u0001');
        sb.append(padColumns("TOTAL:", formatMoney(data.getTotal()), 42)).append('\n');
        sb.append('\u001B').append('E').append('\u0000');

        sb.append(drawLine(42));
        if (data.getPaymentMethods() != null && !data.getPaymentMethods().isEmpty()) {
            sb.append(centerPad("Paid via: " + data.getPaymentMethods(), 42)).append('\n');
        }

        sb.append('\n');
        sb.append(centerPad(data.getFooterText(), 42)).append('\n');
        sb.append(centerPad(data.getThankYou(), 42)).append('\n');
        sb.append(drawLine(42));

        sb.append('\n').append('\n').append('\n');
        sb.append('\u001D').append('V').append('\u0042').append('\u0000');

        return sb.toString();
    }

    private String getSetting(List<SystemSettings> settings, String key, String defaultVal) {
        return settings.stream()
                .filter(s -> key.equals(s.getSettingKey()))
                .findFirst()
                .map(SystemSettings::getSettingValue)
                .orElse(defaultVal);
    }

    private String centerPad(String text, int width) {
        if (text == null) return "";
        int padding = width - text.length();
        if (padding <= 0) return text;
        int leftPad = padding / 2 - 1;
        int rightPad = padding - leftPad - 2;
        return " ".repeat(Math.max(0, leftPad)) + text + " ".repeat(Math.max(0, rightPad)) + "\n";
    }

    private String padColumns(String left, String right, int width) {
        String combined = left + " " + right;
        int padding = width - combined.length();
        if (padding <= 0) return left + " " + right;
        return left + " ".repeat(padding) + right;
    }

    private String drawLine(int width) {
        return "-".repeat(width) + "\n";
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("%.2f", amount != null ? amount : BigDecimal.ZERO);
    }

    private String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(DateTimeFormatter.ofPattern("dd/MM/yy HH:mm"));
    }
}
