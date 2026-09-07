package erp_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "ExpiryBatch")
public class ExpiryBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BatchID")
    private Integer batchId;

    @ManyToOne
    @JoinColumn(name = "ProductID", nullable = false)
    private FinishedProduct product;

    // The date this batch was produced / put into stock. ExpiryDate is
    // always derived from this (see ExpiryCalculator) rather than typed in
    // by hand, so every batch of the same product can still carry its own
    // production date and its own expiry date instead of one fixed expiry
    // for the whole product.
    @Column(name = "ProductionDate")
    private LocalDate productionDate;

    @Column(name = "ExpiryDate", nullable = false)
    private LocalDate expiryDate;

    // Remaining quantity in this specific batch. Reduced (FIFO, soonest
    // expiry first) whenever stock leaves via a Stock Out movement, a sale,
    // or a damaged-stock record, so this always reflects what's actually
    // left of this batch rather than the original amount produced.
    @Column(name = "Quantity", nullable = false)
    private Double quantity;

    // Production cost per unit for this specific batch. Together with
    // ExpiryDate, this is what differentiates one finished-product batch
    // from another the same way GRN's UnitCost + DateReceived does for
    // raw materials, instead of FinishedProduct's single pooled CurrentStock.
    @Column(name = "UnitCost")
    private Double unitCost;

    public Integer getBatchId() { return batchId; }
    public void setBatchId(Integer batchId) { this.batchId = batchId; }
    public FinishedProduct getProduct() { return product; }
    public void setProduct(FinishedProduct product) { this.product = product; }
    public LocalDate getProductionDate() { return productionDate; }
    public void setProductionDate(LocalDate productionDate) { this.productionDate = productionDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    public Double getUnitCost() { return unitCost; }
    public void setUnitCost(Double unitCost) { this.unitCost = unitCost; }
}
