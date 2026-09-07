package erp_backend.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "InvoiceItem")
public class InvoiceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "InvoiceItemID")
    private Integer invoiceItemId;

    @ManyToOne
    @JoinColumn(name = "InvoiceID", nullable = false)
    private Invoice invoice;

    @ManyToOne
    @JoinColumn(name = "ProductID", nullable = false)
    private FinishedProduct product;

    // Quantity, UnitPrice and Subtotal are all DECIMAL(10,2) columns in
    // MySQL. Mapping them through Java's Double (as this used to) reads and
    // writes them via binary floating-point, which can't represent most
    // 2-decimal-place values exactly (e.g. 19.99) - that drift is what was
    // showing up as invoice amounts a cent higher than what was entered.
    // BigDecimal carries the exact decimal value end-to-end instead.
    @Column(name = "Quantity", nullable = false)
    private BigDecimal quantity;

    @Column(name = "UnitPrice", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "Subtotal")
    private BigDecimal subtotal;

    public Integer getInvoiceItemId() { return invoiceItemId; }
    public void setInvoiceItemId(Integer invoiceItemId) { this.invoiceItemId = invoiceItemId; }
    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }
    public FinishedProduct getProduct() { return product; }
    public void setProduct(FinishedProduct product) { this.product = product; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
