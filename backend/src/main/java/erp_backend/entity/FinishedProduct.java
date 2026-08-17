package erp_backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "FinishedProduct")
public class FinishedProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProductID")
    private Integer productId;

    @Column(name = "Name", nullable = false)
    private String name;

    @Column(name = "Category")
    private String category;

    @Column(name = "UnitOfMeasure")
    private String unitOfMeasure;

    @Column(name = "CurrentStock")
    private Double currentStock = 0.0;

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String unitOfMeasure) { this.unitOfMeasure = unitOfMeasure; }
    public Double getCurrentStock() { return currentStock; }
    public void setCurrentStock(Double currentStock) { this.currentStock = currentStock; }
}