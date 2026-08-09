package erp_backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "RawMaterial")
public class RawMaterial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RawMaterialID")
    private Integer rawMaterialId;

    @Column(name = "Name", nullable = false)
    private String name;

    @Column(name = "CurrentStock")
    private Double currentStock = 0.0;

    @Column(name = "ReorderLevel")
    private Double reorderLevel = 0.0;

    public Integer getRawMaterialId() { return rawMaterialId; }
    public void setRawMaterialId(Integer rawMaterialId) { this.rawMaterialId = rawMaterialId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getCurrentStock() { return currentStock; }
    public void setCurrentStock(Double currentStock) { this.currentStock = currentStock; }
    public Double getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(Double reorderLevel) { this.reorderLevel = reorderLevel; }
}