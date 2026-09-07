package com.csi.erpfrontend;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Frontend mirror of the backend's erp_backend.util.ExpiryCalculator rule
 * (Milk Packets -> +2 months, Yogurt -> +3 months, everything else -> +1
 * year) — used only to show a live "this is what the expiry date will be"
 * preview as the user picks a product and production date. The batch's
 * real, authoritative expiry date is always the one computed and saved by
 * the backend; this is a convenience preview, not a second source of truth.
 */
final class ExpiryRules {
    private ExpiryRules() {}

    static LocalDate calculate(String category, String name, LocalDate fromDate) {
        if (fromDate == null) fromDate = LocalDate.now();
        String c = category == null ? "" : category.toLowerCase(Locale.ENGLISH);
        String n = name == null ? "" : name.toLowerCase(Locale.ENGLISH);
        if (c.contains("milk") || n.contains("milk")) return fromDate.plusMonths(2);
        if (c.contains("yog") || n.contains("yog")) return fromDate.plusMonths(3);
        return fromDate.plusYears(1);
    }
}
