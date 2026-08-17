package erp_backend.service;

import erp_backend.entity.*;
import erp_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    public Customer createCustomer(String name, String contactNo, Double creditLimit) {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setContactNo(contactNo);
        customer.setCreditLimit(creditLimit);
        customer.setOutstandingBalance(0.0);
        return customerRepository.save(customer);
    }

    public Invoice createInvoiceWithItems(Customer customer, AppUser salesOfficer, Invoice.PaymentType paymentType,
                                           List<FinishedProduct> products, List<Double> quantities, List<Double> unitPrices) {
        Invoice invoice = new Invoice();
        invoice.setCustomer(customer);
        invoice.setSalesOfficer(salesOfficer);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setPaymentType(paymentType);
        invoice.setTotalAmount(0.0);
        invoice = invoiceRepository.save(invoice);

        double total = 0.0;
        for (int i = 0; i < products.size(); i++) {
            FinishedProduct product = products.get(i);
            double quantity = quantities.get(i);
            double unitPrice = unitPrices.get(i);
            double subtotal = quantity * unitPrice;

            InvoiceItem item = new InvoiceItem();
            item.setInvoice(invoice);
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setUnitPrice(unitPrice);
            item.setSubtotal(subtotal);
            invoiceItemRepository.save(item);

            product.setCurrentStock(product.getCurrentStock() - quantity);
            finishedProductRepository.save(product);

            total += subtotal;
        }

        invoice.setTotalAmount(total);
        invoiceRepository.save(invoice);

        if (paymentType == Invoice.PaymentType.Credit) {
            customer.setOutstandingBalance(customer.getOutstandingBalance() + total);
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

    public CreditCollection recordCollection(Invoice invoice, Double amount) {
        CreditCollection collection = new CreditCollection();
        collection.setInvoice(invoice);
        collection.setAmountCollected(amount);
        collection.setCollectionDate(LocalDate.now());
        creditCollectionRepository.save(collection);

        Customer customer = invoice.getCustomer();
        customer.setOutstandingBalance(customer.getOutstandingBalance() - amount);
        customerRepository.save(customer);

        return collection;
    }
}