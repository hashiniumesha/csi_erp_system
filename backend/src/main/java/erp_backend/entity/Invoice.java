package erp_backend.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "Invoice")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "InvoiceID")
    private Integer invoiceId;

    @ManyToOne
    @JoinColumn(name = "CustomerID", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "SalesOfficerID", nullable = false)
    private AppUser salesOfficer;

    @Column(name = "InvoiceDate", nullable = false)
    private LocalDate invoiceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "PaymentType", nullable = false)
    private PaymentType paymentType;

    // BigDecimal, not Double - this column is DECIMAL(10,2) in MySQL, and
    // reading/writing it through a Double introduces binary floating-point
    // drift (e.g. 19.99 isn't exactly representable in binary) that showed
    // up as invoice totals landing a cent off from what was actually
    // entered. BigDecimal round-trips exact decimal values.
    @Column(name = "TotalAmount")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    public enum PaymentType { Cash, Credit }

    public Integer getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Integer invoiceId) { this.invoiceId = invoiceId; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public AppUser getSalesOfficer() { return salesOfficer; }
    public void setSalesOfficer(AppUser salesOfficer) { this.salesOfficer = salesOfficer; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    public PaymentType getPaymentType() { return paymentType; }
    public void setPaymentType(PaymentType paymentType) { this.paymentType = paymentType; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}