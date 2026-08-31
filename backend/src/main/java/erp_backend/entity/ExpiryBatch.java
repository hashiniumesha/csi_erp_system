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

    @Column(name = "ExpiryDate", nullable = false)
    private LocalDate expiryDate;

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
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    public Double getUnitCost() { return unitCost; }
    public void setUnitCost(Double unitCost) { this.unitCost = unitCost; }
}