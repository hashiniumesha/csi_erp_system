package erp_backend.controller;

import erp_backend.entity.GRN;
import erp_backend.entity.Invoice;
import erp_backend.repository.GRNRepository;
import erp_backend.repository.InvoiceRepository;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Profit & Loss report - per proposal section 5.7 ("Monthly income vs.
 * expenditure analysis", Admin access only), extended to also cover Weekly
 * and Yearly periods on the same report rather than three separate report
 * types. Revenue = total invoice value in the period. Expenditure =
 * approved GRN raw-material cost in the period. This is deliberately not a
 * full accounting P&L - there's no labor/overhead/damaged-stock cost
 * tracked anywhere in the schema - and the PDF says so on every copy rather
 * than quietly presenting a partial figure as if it were complete.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private GRNRepository grnRepository;

    public enum PeriodType { WEEK, MONTH, YEAR }

    // One row of the breakdown table in the PDF/JSON - daily for Week and
    // Month, monthly for Year (365 daily rows in a yearly report would be
    // unreadable, so a year's breakdown rolls up to one row per month).
    public static class BreakdownRow {
        public String label;
        public double revenue;
        public double expenditure;
        public double netResult;

        public BreakdownRow(String label, double revenue, double expenditure) {
            this.label = label;
            this.revenue = revenue;
            this.expenditure = expenditure;
            this.netResult = revenue - expenditure;
        }

        public String getLabel() { return label; }
        public double getRevenue() { return revenue; }
        public double getExpenditure() { return expenditure; }
        public double getNetResult() { return netResult; }
    }

    public static class ProfitLossReport {
        public String periodType;
        public String periodLabel;
        public double totalRevenue;
        public double totalExpenditure;
        public double netResult;
        public List<BreakdownRow> breakdown;
    }

    @GetMapping("/profit-loss")
    public ProfitLossReport profitLoss(
            @RequestParam(defaultValue = "MONTH") String periodType,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String weekStart) {
        return buildReport(PeriodType.valueOf(periodType.toUpperCase(Locale.ENGLISH)), year, month, weekStart);
    }

    @GetMapping("/profit-loss/pdf")
    public ResponseEntity<byte[]> profitLossPdf(
            @RequestParam(defaultValue = "MONTH") String periodType,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String weekStart) throws Exception {
        ProfitLossReport report = buildReport(PeriodType.valueOf(periodType.toUpperCase(Locale.ENGLISH)), year, month, weekStart);

        try (InputStream jrxml = getClass().getClassLoader().getResourceAsStream("reports/profit_loss.jrxml");
             InputStream logo = getClass().getClassLoader().getResourceAsStream("csi-logo.png")) {
            if (jrxml == null) {
                throw new RuntimeException("Report template not found on classpath");
            }
            JasperReport jasperReport = JasperCompileManager.compileReport(jrxml);

            Map<String, Object> params = new HashMap<>();
            params.put("logoImage", logo);
            params.put("reportTypeLabel", reportTypeLabel(report.periodType));
            params.put("periodLabel", report.periodLabel);
            params.put("generatedAt", LocalDate.now().toString());
            params.put("totalRevenue", report.totalRevenue);
            params.put("totalExpenditure", report.totalExpenditure);
            params.put("netProfit", report.netResult);

            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport, params, new JRBeanCollectionDataSource(report.breakdown));

            byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);

            String filename = "profit-loss-" + report.periodType.toLowerCase(Locale.ENGLISH) + "-" +
                    report.periodLabel.replaceAll("[^A-Za-z0-9]+", "-") + ".pdf";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    org.springframework.http.ContentDisposition.attachment().filename(filename).build());
            return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
        }
    }

    private String reportTypeLabel(String periodType) {
        return switch (PeriodType.valueOf(periodType)) {
            case WEEK -> "Weekly Profit & Loss Report";
            case MONTH -> "Monthly Profit & Loss Report";
            case YEAR -> "Yearly Profit & Loss Report";
        };
    }

    private ProfitLossReport buildReport(PeriodType periodType, Integer year, Integer month, String weekStart) {
        LocalDate rangeStart;
        LocalDate rangeEnd;
        String periodLabel;
        boolean monthlyBreakdown = false; // true only for YEAR - daily otherwise

        switch (periodType) {
            case WEEK -> {
                LocalDate start = weekStart != null && !weekStart.isBlank()
                        ? LocalDate.parse(weekStart)
                        : LocalDate.now().with(DayOfWeek.MONDAY);
                rangeStart = start;
                rangeEnd = start.plusDays(6);
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMM yyyy");
                periodLabel = "Week of " + rangeStart.format(fmt) + " – " + rangeEnd.format(fmt);
            }
            case YEAR -> {
                int y = year != null ? year : LocalDate.now().getYear();
                rangeStart = LocalDate.of(y, 1, 1);
                rangeEnd = LocalDate.of(y, 12, 31);
                periodLabel = "Year " + y;
                monthlyBreakdown = true;
            }
            default -> { // MONTH
                int y = year != null ? year : LocalDate.now().getYear();
                int m = month != null ? month : LocalDate.now().getMonthValue();
                rangeStart = LocalDate.of(y, m, 1);
                rangeEnd = rangeStart.withDayOfMonth(rangeStart.lengthOfMonth());
                periodLabel = rangeStart.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + y;
            }
        }

        List<Invoice> invoices = invoiceRepository.findAll().stream()
                .filter(i -> i.getInvoiceDate() != null
                        && !i.getInvoiceDate().isBefore(rangeStart) && !i.getInvoiceDate().isAfter(rangeEnd))
                .toList();

        List<GRN> grns = grnRepository.findAll().stream()
                .filter(g -> g.getStatus() == GRN.Status.Approved
                        && g.getDateReceived() != null
                        && !g.getDateReceived().isBefore(rangeStart) && !g.getDateReceived().isAfter(rangeEnd))
                .toList();

        List<BreakdownRow> breakdown;
        double totalRevenue;
        double totalExpenditure;

        if (monthlyBreakdown) {
            Map<YearMonth, Double> revenueByMonth = new TreeMap<>();
            for (Invoice i : invoices) {
                revenueByMonth.merge(YearMonth.from(i.getInvoiceDate()),
                        i.getTotalAmount() != null ? i.getTotalAmount().doubleValue() : 0.0, Double::sum);
            }
            Map<YearMonth, Double> expenditureByMonth = new TreeMap<>();
            for (GRN g : grns) {
                double cost = (g.getUnitCost() != null ? g.getUnitCost() : 0.0)
                        * (g.getQuantityReceived() != null ? g.getQuantityReceived() : 0.0);
                expenditureByMonth.merge(YearMonth.from(g.getDateReceived()), cost, Double::sum);
            }
            breakdown = new ArrayList<>();
            for (int m = 1; m <= 12; m++) {
                YearMonth ym = YearMonth.of(rangeStart.getYear(), m);
                double revenue = revenueByMonth.getOrDefault(ym, 0.0);
                double expenditure = expenditureByMonth.getOrDefault(ym, 0.0);
                breakdown.add(new BreakdownRow(ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), revenue, expenditure));
            }
            totalRevenue = revenueByMonth.values().stream().mapToDouble(Double::doubleValue).sum();
            totalExpenditure = expenditureByMonth.values().stream().mapToDouble(Double::doubleValue).sum();
        } else {
            Map<LocalDate, Double> revenueByDay = new TreeMap<>();
            for (Invoice i : invoices) {
                revenueByDay.merge(i.getInvoiceDate(),
                        i.getTotalAmount() != null ? i.getTotalAmount().doubleValue() : 0.0, Double::sum);
            }
            Map<LocalDate, Double> expenditureByDay = new TreeMap<>();
            for (GRN g : grns) {
                double cost = (g.getUnitCost() != null ? g.getUnitCost() : 0.0)
                        * (g.getQuantityReceived() != null ? g.getQuantityReceived() : 0.0);
                expenditureByDay.merge(g.getDateReceived(), cost, Double::sum);
            }
            Set<LocalDate> allDays = new TreeSet<>();
            allDays.addAll(revenueByDay.keySet());
            allDays.addAll(expenditureByDay.keySet());
            breakdown = new ArrayList<>();
            for (LocalDate day : allDays) {
                breakdown.add(new BreakdownRow(day.toString(), revenueByDay.getOrDefault(day, 0.0), expenditureByDay.getOrDefault(day, 0.0)));
            }
            totalRevenue = revenueByDay.values().stream().mapToDouble(Double::doubleValue).sum();
            totalExpenditure = expenditureByDay.values().stream().mapToDouble(Double::doubleValue).sum();
        }

        ProfitLossReport report = new ProfitLossReport();
        report.periodType = periodType.name();
        report.periodLabel = periodLabel;
        report.totalRevenue = totalRevenue;
        report.totalExpenditure = totalExpenditure;
        report.netResult = totalRevenue - totalExpenditure;
        report.breakdown = breakdown;
        return report;
    }
}
