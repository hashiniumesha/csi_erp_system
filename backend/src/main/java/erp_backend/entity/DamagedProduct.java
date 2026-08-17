package erp_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "DamagedProduct")
public class DamagedProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DamageID")
    private Integer damageId;

    @ManyToOne
    @JoinColumn(name = "ProductID", nullable = false)
    private FinishedProduct product;

    @Column(name = "Quantity", nullable = false)
    private Double quantity;

    @Column(name = "Cause")
    private String cause;

    @Enumerated(EnumType.STRING)
    @Column(name = "Stage")
    private Stage stage;

    @Column(name = "DamageDate", nullable = false)
    private LocalDate damageDate;

    public enum Stage { Production, PostProduction }

    public Integer getDamageId() { return damageId; }
    public void setDamageId(Integer damageId) { this.damageId = damageId; }
    public FinishedProduct getProduct() { return product; }
    public void setProduct(FinishedProduct product) { this.product = product; }
    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    public String getCause() { return cause; }
    public void setCause(String cause) { this.cause = cause; }
    public Stage getStage() { return stage; }
    public void setStage(Stage stage) { this.stage = stage; }
    public LocalDate getDamageDate() { return damageDate; }
    public void setDamageDate(LocalDate damageDate) { this.damageDate = damageDate; }
}