package erp_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "GRN")
public class GRN {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GRNID")
    private Integer grnId;

    @ManyToOne
    @JoinColumn(name = "SupplierID", nullable = false)
    private Supplier supplier;

    @ManyToOne
    @JoinColumn(name = "RawMaterialID", nullable = false)
    private RawMaterial rawMaterial;

    @Column(name = "QuantityReceived", nullable = false)
    private Double quantityReceived;

    @Column(name = "UnitCost", nullable = false)
    private Double unitCost;

    @Column(name = "DateReceived", nullable = false)
    private LocalDate dateReceived;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status")
    private Status status = Status.Pending;

    public enum Status { Pending, Approved, Rejected }

    public Integer getGrnId() { return grnId; }
    public void setGrnId(Integer grnId) { this.grnId = grnId; }
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    public RawMaterial getRawMaterial() { return rawMaterial; }
    public void setRawMaterial(RawMaterial rawMaterial) { this.rawMaterial = rawMaterial; }
    public Double getQuantityReceived() { return quantityReceived; }
    public void setQuantityReceived(Double quantityReceived) { this.quantityReceived = quantityReceived; }
    public Double getUnitCost() { return unitCost; }
    public void setUnitCost(Double unitCost) { this.unitCost = unitCost; }
    public LocalDate getDateReceived() { return dateReceived; }
    public void setDateReceived(LocalDate dateReceived) { this.dateReceived = dateReceived; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}