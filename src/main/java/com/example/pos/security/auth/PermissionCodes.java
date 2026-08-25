package com.example.pos.security.auth;

import java.util.List;
import java.util.Map;

public final class PermissionCodes {

    public static final String DASHBOARD_READ = "dashboard.read";
    public static final String POS_SELL = "pos.sell";
    public static final String POS_DISCOUNT_REQUEST = "pos.discount.request";
    public static final String POS_DISCOUNT_APPROVE = "pos.discount.approve";
    public static final String SALE_READ = "sale.read";
    public static final String SALE_RECEIPT_REPRINT = "sale.receipt.reprint";
    public static final String SALE_VOID = "sale.void";
    public static final String SALE_RETURN = "sale.return";
    public static final String MEDICINE_READ = "medicine.read";
    public static final String MEDICINE_WRITE = "medicine.write";
    public static final String MEDICINE_PRICE_WRITE = "medicine.price.write";
    public static final String INVENTORY_READ = "inventory.read";
    public static final String INVENTORY_RECEIVE = "inventory.receive";
    public static final String INVENTORY_ADJUST_REQUEST = "inventory.adjust.request";
    public static final String INVENTORY_ADJUST_APPROVE = "inventory.adjust.approve";
    public static final String SUPPLIER_READ = "supplier.read";
    public static final String SUPPLIER_WRITE = "supplier.write";
    public static final String CUSTOMER_READ = "customer.read";
    public static final String CUSTOMER_WRITE = "customer.write";
    public static final String PURCHASE_ORDER_READ = "purchase_order.read";
    public static final String PURCHASE_ORDER_WRITE = "purchase_order.write";
    public static final String SHIFT_OPEN = "shift.open";
    public static final String SHIFT_CLOSE = "shift.close";
    public static final String SHIFT_VARIANCE_APPROVE = "shift.variance.approve";
    public static final String REPORT_SALES_READ = "report.sales.read";
    public static final String REPORT_INVENTORY_READ = "report.inventory.read";
    public static final String USER_MANAGE = "user.manage";
    public static final String SETTINGS_MANAGE = "settings.manage";
    public static final String AUDIT_READ = "audit.read";
    public static final String TERMINAL_READ = "terminal.read";
    public static final String TERMINAL_MANAGE = "terminal.manage";
    public static final String PRESCRIPTION_READ = "prescription.read";
    public static final String PRESCRIPTION_APPROVE = "prescription.approve";

    public static final List<String> ALL = List.of(
            DASHBOARD_READ,
            POS_SELL,
            POS_DISCOUNT_REQUEST,
            POS_DISCOUNT_APPROVE,
            SALE_READ,
            SALE_RECEIPT_REPRINT,
            SALE_VOID,
            SALE_RETURN,
            MEDICINE_READ,
            MEDICINE_WRITE,
            MEDICINE_PRICE_WRITE,
            INVENTORY_READ,
            INVENTORY_RECEIVE,
            INVENTORY_ADJUST_REQUEST,
            INVENTORY_ADJUST_APPROVE,
            SUPPLIER_READ,
            SUPPLIER_WRITE,
            CUSTOMER_READ,
            CUSTOMER_WRITE,
            PURCHASE_ORDER_READ,
            PURCHASE_ORDER_WRITE,
            SHIFT_OPEN,
            SHIFT_CLOSE,
            SHIFT_VARIANCE_APPROVE,
            REPORT_SALES_READ,
            REPORT_INVENTORY_READ,
            USER_MANAGE,
            SETTINGS_MANAGE,
            AUDIT_READ,
            TERMINAL_READ,
            TERMINAL_MANAGE,
            PRESCRIPTION_READ,
            PRESCRIPTION_APPROVE);

    public static final Map<String, List<String>> ROLE_BUNDLES = Map.of(
            "OWNER", ALL.stream().filter(code -> !PRESCRIPTION_APPROVE.equals(code)).toList(),
            "BRANCH_MANAGER", List.of(
                    DASHBOARD_READ, POS_DISCOUNT_APPROVE, SALE_READ, SALE_RECEIPT_REPRINT,
                    SALE_VOID, SALE_RETURN, MEDICINE_READ, MEDICINE_WRITE,
                    MEDICINE_PRICE_WRITE, INVENTORY_READ,
                    INVENTORY_ADJUST_APPROVE, SUPPLIER_READ, CUSTOMER_READ,
                    CUSTOMER_WRITE, PURCHASE_ORDER_READ, SHIFT_VARIANCE_APPROVE,
                    REPORT_SALES_READ, REPORT_INVENTORY_READ, TERMINAL_READ,
                    TERMINAL_MANAGE, PRESCRIPTION_READ),
            "PHARMACIST", List.of(
                    DASHBOARD_READ, POS_SELL, SALE_READ, MEDICINE_READ, INVENTORY_READ,
                    CUSTOMER_READ, CUSTOMER_WRITE,
                    SHIFT_OPEN, SHIFT_CLOSE, PRESCRIPTION_READ, PRESCRIPTION_APPROVE),
            "CASHIER", List.of(
                    POS_SELL, POS_DISCOUNT_REQUEST, SALE_READ, SALE_RECEIPT_REPRINT,
                    SALE_RETURN, MEDICINE_READ, CUSTOMER_READ, CUSTOMER_WRITE,
                    SHIFT_OPEN, SHIFT_CLOSE),
            "STORE_KEEPER", List.of(
                    DASHBOARD_READ, MEDICINE_READ, MEDICINE_WRITE, MEDICINE_PRICE_WRITE,
                    INVENTORY_READ, INVENTORY_RECEIVE,
                    INVENTORY_ADJUST_REQUEST, SUPPLIER_READ, SUPPLIER_WRITE,
                    PURCHASE_ORDER_READ, PURCHASE_ORDER_WRITE, REPORT_INVENTORY_READ),
            "PHARMACY_TECHNICIAN", List.of(
                    DASHBOARD_READ, POS_SELL, SALE_READ, SALE_RECEIPT_REPRINT, MEDICINE_READ,
                    INVENTORY_READ, INVENTORY_RECEIVE, SUPPLIER_READ,
                    CUSTOMER_READ, CUSTOMER_WRITE, PURCHASE_ORDER_READ,
                    SHIFT_OPEN, SHIFT_CLOSE, PRESCRIPTION_READ));

    private PermissionCodes() {
    }
}
