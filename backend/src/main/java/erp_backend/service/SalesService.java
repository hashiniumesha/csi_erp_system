package erp_backend.service;

import erp_backend.entity.*;
import erp_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class SalesService {

    @Autowired private CustomerRepository customerRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private InvoiceItemRepository invoiceItemRepository;
    @Autowired private DeliveryRouteRepository deliveryRouteRepository;
    @Autowired private CreditCollectionRepository creditCollectionRepository;
    @Autowired private FinishedProductRepository finishedProductRepository;
    @Autowired private InventoryService inventoryService;

    public Customer createCustomer(String name, String contactNo, BigDecimal creditLimit) {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setContactNo(contactNo);
        customer.setCreditLimit(creditLimit);
        customer.setOutstandingBalance(BigDecimal.ZERO);
        return customerRepository.save(customer);
    }

    public Invoice createInvoiceWithItems(Customer customer, AppUser salesOfficer, Invoice.PaymentType paymentType,
                                           List<FinishedProduct> products, List<BigDecimal> quantities, List<BigDecimal> unitPrices) {
        Invoice invoice = new Invoice();
        invoice.setCustomer(customer);
        invoice.setSalesOfficer(salesOfficer);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setPaymentType(paymentType);
        invoice.setTotalAmount(BigDecimal.ZERO);
        invoice = invoiceRepository.save(invoice);

        // BigDecimal throughout - quantity * unitPrice on doubles is what
        // was landing invoice totals a cent off the amount actually
        // entered (binary floating-point can't represent most 2-decimal
        // values exactly). setScale(2, HALF_UP) matches the DECIMAL(10,2)
        // columns these values are stored in, so what's saved is exactly
        // what was calculated - no silent rounding surprises later.
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < products.size(); i++) {
            FinishedProduct product = products.get(i);
            BigDecimal quantity = quantities.get(i);
            BigDecimal unitPrice = unitPrices.get(i);
            BigDecimal subtotal = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

            InvoiceItem item = new InvoiceItem();
            item.setInvoice(invoice);
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setUnitPrice(unitPrice);
            item.setSubtotal(subtotal);
            invoiceItemRepository.save(item);

            product.setCurrentStock(product.getCurrentStock() - quantity.doubleValue());
            finishedProductRepository.save(product);
            // A sale is stock leaving too - deplete it from whichever
            // expiry batches actually hold it, soonest-expiring first.
            inventoryService.depleteExpiryBatchesFifo(product, quantity.doubleValue());

            total = total.add(subtotal);
        }

        invoice.setTotalAmount(total);
        invoiceRepository.save(invoice);

        if (paymentType == Invoice.PaymentType.Credit) {
            customer.setOutstandingBalance(customer.getOutstandingBalance().add(total));
            customerRepository.save(customer);
        }

        return invoice;
    }

    public DeliveryRoute createDeliveryRoute(AppUser salesOfficer, LocalDate routeDate) {
        DeliveryRoute route = new DeliveryRoute();
        route.setSalesOfficer(salesOfficer);
        route.setRouteDate(routeDate);
        return deliveryRouteRepository.save(route);
    }

    public CreditCollection recordCollection(Invoice invoice, BigDecimal amount) {
        CreditCollection collection = new CreditCollection();
        collection.setInvoice(invoice);
        collection.setAmountCollected(amount);
        collection.setCollectionDate(LocalDate.now());
        creditCollectionRepository.save(collection);

        Customer customer = invoice.getCustomer();
        customer.setOutstandingBalance(customer.getOutstandingBalance().subtract(amount));
        customerRepository.save(customer);

        return collection;
    }

    // Full detail for one invoice, including its line items - what the
    // Invoice Preview screen renders (proper invoice document, not just a
    // "created successfully" message).
    public Invoice getInvoice(Integer invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
    }

    public List<InvoiceItem> getInvoiceItems(Integer invoiceId) {
        return invoiceItemRepository.findByInvoice_InvoiceId(invoiceId);
    }
}
