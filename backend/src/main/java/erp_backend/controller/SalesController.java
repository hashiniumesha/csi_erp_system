package erp_backend.controller;

import erp_backend.entity.*;
import erp_backend.repository.*;
import erp_backend.service.SalesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
        public Double creditLimit;
    }

    public static class InvoiceItemRequest {
        public Integer productId;
        public Double quantity;
        public Double unitPrice;
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
        public Double amount;
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
    public Invoice createInvoice(@RequestBody InvoiceRequest request) {
        Customer customer = customerRepository.findById(request.customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        AppUser salesOfficer = appUserRepository.findById(request.salesOfficerId)
                .orElseThrow(() -> new RuntimeException("Sales officer not found"));

        List<FinishedProduct> products = new ArrayList<>();
        List<Double> quantities = new ArrayList<>();
        List<Double> unitPrices = new ArrayList<>();

        for (InvoiceItemRequest itemReq : request.items) {
            FinishedProduct product = finishedProductRepository.findById(itemReq.productId)
                    .orElseThrow(() -> new RuntimeException("Product not found ID: " + itemReq.productId));
            products.add(product);
            quantities.add(itemReq.quantity);
            unitPrices.add(itemReq.unitPrice);
        }

        return salesService.createInvoiceWithItems(
                customer,
                salesOfficer,
                Invoice.PaymentType.valueOf(request.paymentType),
                products,
                quantities,
                unitPrices
        );
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
        public Double totalAmount;
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