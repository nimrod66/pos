package com.example.pos.sale.discount;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DiscountEngine {

    public enum DiscountType {
        PERCENTAGE, FIXED, NONE
    }

    public record Discount(DiscountType type, BigDecimal value, String reason) {

        public static Discount none() {
            return new Discount(DiscountType.NONE, BigDecimal.ZERO, null);
        }

        public static Discount percentage(BigDecimal percent, String reason) {
            if (percent.compareTo(BigDecimal.ZERO) < 0 || percent.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Percentage must be between 0 and 100");
            }
            return new Discount(DiscountType.PERCENTAGE, percent, reason);
        }

        public static Discount fixed(BigDecimal amount, String reason) {
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Fixed discount cannot be negative");
            }
            return new Discount(DiscountType.FIXED, amount, reason);
        }

        public BigDecimal calculate(BigDecimal price, BigDecimal quantity) {
            BigDecimal total = price.multiply(quantity);
            return switch (type) {
                case PERCENTAGE -> total.multiply(value).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                case FIXED -> value.min(total);
                case NONE -> BigDecimal.ZERO;
            };
        }
    }
}
