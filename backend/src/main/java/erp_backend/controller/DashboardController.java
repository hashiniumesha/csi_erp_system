package erp_backend.controller;

import erp_backend.entity.*;
import erp_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired private AppUserRepository appUserRepository;
    @Autowired private GRNRepository grnRepository;
    @Autowired private RawMaterialRepository rawMaterialRepository;
    @Autowired private FinishedProductRepository finishedProductRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private FinishedProductStockMovementRepository finishedProductStockMovementRepository;
    @Autowired private DamagedProductRepository damagedProductRepository;

    public static class DashboardSummary {
        public long totalUsers;
        public long pendingGrnCount;
        public long approvedGrnCount;
        public long totalRawMaterials;
        public long lowStockRawMaterials;
        public long totalFinishedProducts;
        public long lowStockFinishedProducts;
        public long totalCustomers;
        public long invoicesToday;
        public double salesTotalToday;
        public double salesTotalThisMonth;
        public double totalOutstandingBalance;
        public long stockInMovementCount;
        public long stockOutMovementCount;
        public long damagedProductRecordCount;
    }

    @GetMapping("/summary")
    public DashboardSummary summary() {
        DashboardSummary s = new DashboardSummary();

        s.totalUsers = appUserRepository.count();

        List<GRN> grns = grnRepository.findAll();
        s.pendingGrnCount = grns.stream().filter(g -> g.getStatus() == GRN.Status.Pending).count();
        s.approvedGrnCount = grns.stream().filter(g -> g.getStatus() == GRN.Status.Approved).count();

        List<RawMaterial> rawMaterials = rawMaterialRepository.findAll();
        s.totalRawMaterials = rawMaterials.size();
        s.lowStockRawMaterials = rawMaterials.stream()
                .filter(r -> r.getCurrentStock() != null && r.getReorderLevel() != null && r.getCurrentStock() <= r.getReorderLevel())
                .count();

        List<FinishedProduct> products = finishedProductRepository.findAll();
        s.totalFinishedProducts = products.size();
        s.lowStockFinishedProducts = products.stream()
                .filter(p -> p.getCurrentStock() != null && p.getCurrentStock() < 20)
                .count();

        s.totalCustomers = customerRepository.count();

        List<Invoice> invoices = invoiceRepository.findAll();
        LocalDate today = LocalDate.now();
        s.invoicesToday = invoices.stream().filter(i -> today.equals(i.getInvoiceDate())).count();
        s.salesTotalToday = invoices.stream()
                .filter(i -> today.equals(i.getInvoiceDate()))
                .mapToDouble(i -> i.getTotalAmount() != null ? i.getTotalAmount().doubleValue() : 0.0)
                .sum();
        s.salesTotalThisMonth = invoices.stream()
                .filter(i -> i.getInvoiceDate() != null
                        && i.getInvoiceDate().getMonthValue() == today.getMonthValue()
                        && i.getInvoiceDate().getYear() == today.getYear())
                .mapToDouble(i -> i.getTotalAmount() != null ? i.getTotalAmount().doubleValue() : 0.0)
                .sum();

        s.totalOutstandingBalance = customerRepository.findAll().stream()
                .mapToDouble(c -> c.getOutstandingBalance() != null ? c.getOutstandingBalance().doubleValue() : 0.0)
                .sum();

        List<FinishedProductStockMovement> movements = finishedProductStockMovementRepository.findAll();
        s.stockInMovementCount = movements.stream()
                .filter(m -> m.getMovementType() == FinishedProductStockMovement.MovementType.IN).count();
        s.stockOutMovementCount = movements.stream()
                .filter(m -> m.getMovementType() == FinishedProductStockMovement.MovementType.OUT).count();

        s.damagedProductRecordCount = damagedProductRepository.count();

        return s;
    }
}