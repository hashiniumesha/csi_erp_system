package erp_backend.util;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Central place for the shelf-life rules behind expiry-date auto-calculation
 * (Inventory / Production / QC expiry tracking). Given a product's name (and
 * optionally its category) plus the date it was produced/received, works out
 * the one expiry date that applies — the user should never have to compute
 * this by hand.
 *
 * Rules (client-specified):
 *   - Milk Packets  -> 2 months from production/stock date
 *   - Yogurt        -> 3 months from production/stock date
 *   - Everything else -> 1 year from production/stock date
 *
 * Matching is done on category first (exact dropdown values used in
 * Inventory, e.g. "Milk Packets", "Yogurt"), falling back to a
 * case-insensitive keyword match on the product/material name so this still
 * works for raw materials, which have no category field.
 */
public final class ExpiryCalculator {

    private ExpiryCalculator() {}

    /** How many days out counts as "approaching expiry" rather than just "tracked". */
    public static final int EXPIRING_SOON_WINDOW_DAYS = 14;

    public enum ShelfLifeRule {
        MILK(2, "months", "Milk Packets"),
        YOGURT(3, "months", "Yogurt"),
        STANDARD(1, "years", "Standard (1 year)");

        public final int amount;
        public final String unit; // "months" or "years"
        public final String label;

        ShelfLifeRule(int amount, String unit, String label) {
            this.amount = amount;
            this.unit = unit;
            this.label = label;
        }
    }

    public static ShelfLifeRule ruleFor(String category, String name) {
        String c = category == null ? "" : category.toLowerCase(Locale.ENGLISH);
        String n = name == null ? "" : name.toLowerCase(Locale.ENGLISH);
        if (c.contains("milk") || n.contains("milk")) return ShelfLifeRule.MILK;
        if (c.contains("yog") || n.contains("yog")) return ShelfLifeRule.YOGURT; // covers "yogurt" and "yoghurt"
        return ShelfLifeRule.STANDARD;
    }

    public static LocalDate calculateExpiryDate(String category, String name, LocalDate fromDate) {
        if (fromDate == null) fromDate = LocalDate.now();
        ShelfLifeRule rule = ruleFor(category, name);
        return "months".equals(rule.unit) ? fromDate.plusMonths(rule.amount) : fromDate.plusYears(rule.amount);
    }

    public enum ExpiryStatus { EXPIRED, EXPIRING_SOON, OK }

    public static ExpiryStatus statusOf(LocalDate expiryDate) {
        if (expiryDate == null) return ExpiryStatus.OK;
        LocalDate today = LocalDate.now();
        if (expiryDate.isBefore(today)) return ExpiryStatus.EXPIRED;
        if (!expiryDate.isAfter(today.plusDays(EXPIRING_SOON_WINDOW_DAYS))) return ExpiryStatus.EXPIRING_SOON;
        return ExpiryStatus.OK;
    }

    /** Whole days from today until expiry. Negative means already expired. */
    public static long daysRemaining(LocalDate expiryDate) {
        if (expiryDate == null) return Long.MAX_VALUE;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }
}
