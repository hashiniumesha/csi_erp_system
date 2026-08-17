package erp_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "RawMaterialStockMovement")
public class RawMaterialStockMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MovementID")
    private Integer movementId;

    @ManyToOne
    @JoinColumn(name = "RawMaterialID", nullable = false)
    private RawMaterial rawMaterial;

    @Enumerated(EnumType.STRING)
    @Column(name = "MovementType", nullable = false)
    private MovementType movementType;

    @Column(name = "Quantity", nullable = false)
    private Double quantity;

    @Column(name = "MovementDate", nullable = false)
    private LocalDate movementDate;

    @Column(name = "ReferenceType")
    private String referenceType;

    @Column(name = "ReferenceID")
    private Integer referenceId;

    public enum MovementType { IN, OUT }

    public Integer getMovementId() { return movementId; }
    public void setMovementId(Integer movementId) { this.movementId = movementId; }
    public RawMaterial getRawMaterial() { return rawMaterial; }
    public void setRawMaterial(RawMaterial rawMaterial) { this.rawMaterial = rawMaterial; }
    public MovementType getMovementType() { return movementType; }
    public void setMovementType(MovementType movementType) { this.movementType = movementType; }
    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    public LocalDate getMovementDate() { return movementDate; }
    public void setMovementDate(LocalDate movementDate) { this.movementDate = movementDate; }
    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public Integer getReferenceId() { return referenceId; }
    public void setReferenceId(Integer referenceId) { this.referenceId = referenceId; }
}