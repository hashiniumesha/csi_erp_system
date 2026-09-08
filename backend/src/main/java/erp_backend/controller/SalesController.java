package erp_backend.controller;

import erp_backend.entity.*;
import erp_backend.repository.*;
import erp_backend.service.SalesService;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
public class SalesController {

    @Autowired private SalesService salesService;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private FinishedProductRepository finishedProductRepository;

    public static class CustomerRequest {
        public String name;
        public String contactNo;
        public BigDecimal creditLimit;
    }

    public static class InvoiceItemRequest {
        public Integer productId;
        public BigDecimal quantity;
        public BigDecimal unitPrice;
    }

    public static class InvoiceRequest {
        public Integer customerId;
        public Integer salesOfficerId;
        public String paymentType;
        public List<InvoiceItemRequest> items;
    }

    public static class RouteRequest {
        public Integer salesOfficerId;
        public String routeDate;
    }

    public static class CollectionRequest {
        public Integer invoiceId;
        public BigDecimal amount;
    }

    @PostMapping("/customer")
    public Customer createCustomer(@RequestBody CustomerRequest request) {
        return salesService.createCustomer(request.name, request.contactNo, request.creditLimit);
    }

    @GetMapping("/customers")
    public List<Customer> listCustomers() {
        return customerRepository.findAll();
    }

    @PostMapping("/invoice")
    public InvoiceDetail createInvoice(@RequestBody InvoiceRequest request) {
        Customer customer = customerRepository.findById(request.customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        AppUser salesOfficer = appUserRepository.findById(request.salesOfficerId)
                .orElseThrow(() -> new RuntimeException("Sales officer not found"));

        List<FinishedProduct> products = new ArrayList<>();
        List<BigDecimal> quantities = new ArrayList<>();
        List<BigDecimal> unitPrices = new ArrayList<>();

        for (InvoiceItemRequest itemReq : request.items) {
            FinishedProduct product = finishedProductRepository.findById(itemReq.productId)
                    .orElseThrow(() -> new RuntimeException("Product not found ID: " + itemReq.productId));
            products.add(product);
            quantities.add(itemReq.quantity);
            unitPrices.add(itemReq.unitPrice);
        }

        Invoice invoice = salesService.createInvoiceWithItems(
                customer,
                salesOfficer,
                Invoice.PaymentType.valueOf(request.paymentType),
                products,
                quantities,
                unitPrices
        );
        // Returning the raw Invoice entity here used to serialize its
        // nested salesOfficer (AppUser) straight into the response,
        // passwordHash included — same leak the /invoices list endpoint
        // was already fixed for. Build the same safe detail DTO the
        // Invoice Preview screen uses instead, so this response can go
        // straight into that preview.
        return getInvoiceDetail(invoice.getInvoiceId());
    }

    // Safe projection — the raw Invoice entity nests the full AppUser as
    // salesOfficer, which was serializing passwordHash straight into the
    // JSON response (the same reason every other list endpoint in this
    // app avoids returning entities that carry a nested AppUser directly).
    public static class InvoiceSummary {
        public Integer invoiceId;
        public String customerName;
        public String salesOfficerName;
        public String invoiceDate;
        public String paymentType;
        public BigDecimal totalAmount;
    }

    @GetMapping("/invoices")
    public List<InvoiceSummary> listInvoices() {
        return invoiceRepository.findAll().stream().map(i -> {
            InvoiceSummary s = new InvoiceSummary();
            s.invoiceId = i.getInvoiceId();
            s.customerName = i.getCustomer() != null ? i.getCustomer().getName() : "?";
            s.salesOfficerName = i.getSalesOfficer() != null ? i.getSalesOfficer().getFullName() : "?";
            s.invoiceDate = i.getInvoiceDate() != null ? i.getInvoiceDate().toString() : null;
            s.paymentType = i.getPaymentType() != null ? i.getPaymentType().name() : null;
            s.totalAmount = i.getTotalAmount();
            return s;
        }).toList();
    }

    // Full invoice document - customer, officer, and every line item - for
    // the Invoice Preview screen. Kept as a safe projection for the same
    // reason InvoiceSummary is: the raw entity nests a full AppUser
    // (salesOfficer) whose JSON serialization leaks passwordHash.
    public static class InvoiceItemView {
        public String productName;
        public String unitOfMeasure;
        public BigDecimal quantity;
        public BigDecimal unitPrice;
        public BigDecimal subtotal;

        // JasperReports' JRBeanCollectionDataSource reads rows via JavaBean
        // getters (reflection), not direct public field access - without
        // these the invoice PDF failed with "Unknown property 'productName'"
        // even though the JSON response (Jackson, which does read public
        // fields directly) worked fine.
        public String getProductName() { return productName; }
        public String getUnitOfMeasure() { return unitOfMeasure; }
        public BigDecimal getQuantity() { return quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public BigDecimal getSubtotal() { return subtotal; }
    }

    public static class InvoiceDetail {
        public Integer invoiceId;
        public String invoiceDate;
        public String paymentType;
        public String customerName;
        public String customerContact;
        public String salesOfficerName;
        public List<InvoiceItemView> items;
        public BigDecimal totalAmount;
    }

    @GetMapping("/invoice/{id}")
    public InvoiceDetail getInvoiceDetail(@PathVariable Integer id) {
        Invoice invoice = salesService.getInvoice(id);
        InvoiceDetail d = new InvoiceDetail();
        d.invoiceId = invoice.getInvoiceId();
        d.invoiceDate = invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().toString() : null;
        d.paymentType = invoice.getPaymentType() != null ? invoice.getPaymentType().name() : null;
        d.customerName = invoice.getCustomer() != null ? invoice.getCustomer().getName() : "?";
        d.customerContact = invoice.getCustomer() != null ? invoice.getCustomer().getContactNo() : null;
        d.salesOfficerName = invoice.getSalesOfficer() != null ? invoice.getSalesOfficer().getFullName() : "?";
        d.totalAmount = invoice.getTotalAmount();
        d.items = salesService.getInvoiceItems(id).stream().map(item -> {
            InvoiceItemView v = new InvoiceItemView();
            v.productName = item.getProduct() != null ? item.getProduct().getName() : "?";
            v.unitOfMeasure = item.getProduct() != null ? item.getProduct().getUnitOfMeasure() : null;
            v.quantity = item.getQuantity();
            v.unitPrice = item.getUnitPrice();
            v.subtotal = item.getSubtotal();
            return v;
        }).toList();
        return d;
    }

    // Real, printable PDF - not a screenshot of the on-screen preview. Same
    // JasperReports pattern as the Reports module (compile-at-request-time,
    // fill from real data, export bytes), with the CSI logo embedded via an
    // <image> element fed from an InputStream parameter.
    @GetMapping("/invoice/{id}/pdf")
    public ResponseEntity<byte[]> getInvoicePdf(@PathVariable Integer id) throws Exception {
        InvoiceDetail invoice = getInvoiceDetail(id);

        try (InputStream jrxml = getClass().getClassLoader().getResourceAsStream("reports/invoice.jrxml");
             InputStream logo = getClass().getClassLoader().getResourceAsStream("csi-logo.png")) {
            if (jrxml == null) {
                throw new RuntimeException("Invoice PDF template not found on classpath");
            }
            JasperReport jasperReport = JasperCompileManager.compileReport(jrxml);

            Map<String, Object> params = new HashMap<>();
            params.put("logoImage", logo);
            params.put("invoiceNumber", invoice.invoiceId);
            params.put("invoiceDate", invoice.invoiceDate);
            params.put("paymentType", invoice.paymentType);
            params.put("customerName", invoice.customerName);
            params.put("customerContact", invoice.customerContact);
            params.put("salesOfficerName", invoice.salesOfficerName);
            params.put("totalAmount", invoice.totalAmount);

            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport, params, new JRBeanCollectionDataSource(invoice.items));

            byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);

            String filename = "invoice-" + id + ".pdf";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    org.springframework.http.ContentDisposition.attachment().filename(filename).build());
            return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
        }
    }

    @PostMapping("/route")
    public DeliveryRoute createRoute(@RequestBody RouteRequest request) {
        AppUser salesOfficer = appUserRepository.findById(request.salesOfficerId)
                .orElseThrow(() -> new RuntimeException("Sales officer not found"));

        return salesService.createDeliveryRoute(salesOfficer, LocalDate.parse(request.routeDate));
    }

    @PostMapping("/collection")
    public CreditCollection recordCollection(@RequestBody CollectionRequest request) {
        Invoice invoice = invoiceRepository.findById(request.invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        return salesService.recordCollection(invoice, request.amount);
    }
}
