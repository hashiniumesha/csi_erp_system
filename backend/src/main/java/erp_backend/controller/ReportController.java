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
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Profit & Loss report - per proposal section 5.7 ("Monthly income vs.
 * expenditure analysis", Admin access only). First of the five reports
 * from that section still to be built (the sixth, "Graphical Dashboard",
 * is what DashboardController/DashboardHomeView already deliver).
 *
 * Revenue = total invoice value in the period. Expenditure = approved
 * GRN raw-material cost in the period. This is deliberately not a full
 * accounting P&L - there's no labor/overhead/damaged-stock cost tracked
 * anywhere in the schema - and the PDF says so on every copy rather than
 * quietly presenting a partial figure as if it were complete.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private GRNRepository grnRepository;

    // One row of the daily breakdown table in the PDF/JSON. Public fields
    // (JavaBean-style getters would also work) so JasperReports' bean data
    // source can read them via reflection.
    public static class DailyPL {
        public String date;
        public double revenue;
        public double expenditure;
        public double netProfit;

        public DailyPL(String date, double revenue, double expenditure) {
            this.date = date;
            this.revenue = revenue;
            this.expenditure = expenditure;
            this.netProfit = revenue - expenditure;
        }

        public String getDate() { return date; }
        public double getRevenue() { return revenue; }
        public double getExpenditure() { return expenditure; }
        public double getNetProfit() { return netProfit; }
    }

    public static class ProfitLossReport {
        public String periodLabel;
        public double totalRevenue;
        public double totalExpenditure;
        public double netProfit;
        public List<DailyPL> dailyBreakdown;
    }

    @GetMapping("/profit-loss")
    public ProfitLossReport profitLoss(@RequestParam int year, @RequestParam int month) {
        return buildReport(year, month);
    }

    @GetMapping("/profit-loss/pdf")
    public ResponseEntity<byte[]> profitLossPdf(@RequestParam int year, @RequestParam int month) throws Exception {
        ProfitLossReport report = buildReport(year, month);

        try (InputStream jrxml = getClass().getClassLoader().getResourceAsStream("reports/profit_loss.jrxml")) {
            if (jrxml == null) {
                throw new RuntimeException("Report template not found on classpath");
            }
            JasperReport jasperReport = JasperCompileManager.compileReport(jrxml);

            Map<String, Object> params = new HashMap<>();
            params.put("periodLabel", report.periodLabel);
            params.put("generatedAt", LocalDate.now().toString());
            params.put("totalRevenue", report.totalRevenue);
            params.put("totalExpenditure", report.totalExpenditure);
            params.put("netProfit", report.netProfit);

            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport, params, new JRBeanCollectionDataSource(report.dailyBreakdown));

            byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);

            String filename = "profit-loss-" + year + "-" + String.format("%02d", month) + ".pdf";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    org.springframework.http.ContentDisposition.attachment().filename(filename).build());
            return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
        }
    }

    private ProfitLossReport buildReport(int year, int month) {
        List<Invoice> invoices = invoiceRepository.findAll().stream()
                .filter(i -> i.getInvoiceDate() != null
                        && i.getInvoiceDate().getYear() == year
                        && i.getInvoiceDate().getMonthValue() == month)
                .toList();

        List<GRN> grns = grnRepository.findAll().stream()
                .filter(g -> g.getStatus() == GRN.Status.Approved
                        && g.getDateReceived() != null
                        && g.getDateReceived().getYear() == year
                        && g.getDateReceived().getMonthValue() == month)
                .toList();

        Map<LocalDate, Double> revenueByDay = new TreeMap<>();
        for (Invoice i : invoices) {
            revenueByDay.merge(i.getInvoiceDate(), i.getTotalAmount() != null ? i.getTotalAmount() : 0.0, Double::sum);
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

        List<DailyPL> daily = new ArrayList<>();
        for (LocalDate day : allDays) {
            daily.add(new DailyPL(day.toString(), revenueByDay.getOrDefault(day, 0.0), expenditureByDay.getOrDefault(day, 0.0)));
        }

        ProfitLossReport report = new ProfitLossReport();
        LocalDate monthStart = LocalDate.of(year, month, 1);
        report.periodLabel = monthStart.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year;
        report.totalRevenue = revenueByDay.values().stream().mapToDouble(Double::doubleValue).sum();
        report.totalExpenditure = expenditureByDay.values().stream().mapToDouble(Double::doubleValue).sum();
        report.netProfit = report.totalRevenue - report.totalExpenditure;
        report.dailyBreakdown = daily;
        return report;
    }
}
