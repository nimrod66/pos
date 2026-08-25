package com.example.pos.core.systemsettings;

public final class SettingKeys {

    private SettingKeys() {
    }

    public static final class Receipt {
        public static final String STORE_NAME = "receipt.store_name";
        public static final String HEADER_TEXT = "receipt.header_text";
        public static final String FOOTER_TEXT = "receipt.footer_text";
        public static final String SHOW_LOGO = "receipt.show_logo";
        public static final String SHOW_BARCODE = "receipt.show_barcode";
        public static final String SHOW_TAX_BREAKDOWN = "receipt.show_tax_breakdown";
        public static final String RETURN_POLICY = "receipt.return_policy";
        public static final String THANK_YOU_MESSAGE = "receipt.thank_you_message";
    }

    public static final class Invoice {
        public static final String PREFIX = "invoice.prefix";
        public static final String STARTING_NUMBER = "invoice.starting_number";
        public static final String RESET_FREQUENCY = "invoice.reset_frequency";
    }

    public static final class Inventory {
        public static final String LOW_STOCK_THRESHOLD = "inventory.low_stock_threshold";
        public static final String EXPIRY_ALERT_DAYS = "inventory.expiry_alert_days";
        public static final String AUTO_REORDER = "inventory.auto_reorder";
        public static final String DEFAULT_REORDER_LEVEL = "inventory.default_reorder_level";
    }

    public static final class Sale {
        public static final String TAX_RATE = "sale.tax_rate";
        public static final String CURRENCY = "sale.currency";
        public static final String MAX_DISCOUNT_PERCENT = "sale.max_discount_percent";
        public static final String REQUIRE_MANAGER_FOR_VOID = "sale.require_manager_for_void";
        public static final String ALLOW_CREDIT_SALES = "sale.allow_credit_sales";
        public static final String ROUNDING_MODE = "sale.rounding_mode";
    }

    public static final class Payment {
        public static final String M_PESA_ENABLED = "payment.mpesa_enabled";
        public static final String CARD_ENABLED = "payment.card_enabled";
        public static final String CASH_ENABLED = "payment.cash_enabled";
        public static final String MPESA_CONSUMER_KEY = "payment.mpesa_consumer_key";
        public static final String MPESA_CONSUMER_SECRET = "payment.mpesa_consumer_secret";
        public static final String MPESA_PASSKEY = "payment.mpesa_passkey";
        public static final String MPESA_SHORTCODE = "payment.mpesa_shortcode";
        public static final String MPESA_ENVIRONMENT = "payment.mpesa_environment";
        public static final String MPESA_CALLBACK_URL = "payment.mpesa_callback_url";
    }

    public static final class Shift {
        public static final String AUTO_CLOSE_HOUR = "shift.auto_close_hour";
        public static final String MAX_VARIANCE_AMOUNT = "shift.max_variance_amount";
        public static final String REQUIRE_COUNT = "shift.require_count";
    }

    public static final class Etims {
        public static final String ENABLED = "etims.enabled";
        public static final String DEVICE_SERIAL = "etims.device_serial";
        public static final String SIGNING_KEY = "etims.signing_key";
    }

    public static final class Hardware {
        public static final String CONNECTOR_URL = "hardware.connector_url";
    }

    public static final class Backup {
        public static final String AUTO_BACKUP = "backup.auto_backup";
        public static final String FREQUENCY_HOURS = "backup.frequency_hours";
    }
}
