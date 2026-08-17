package erp_backend.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "DeliveryRoute")
public class DeliveryRoute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RouteID")
    private Integer routeId;

    @Column(name = "RouteDate", nullable = false)
    private LocalDate routeDate;

    @ManyToOne
    @JoinColumn(name = "SalesOfficerID", nullable = false)
    private AppUser salesOfficer;

    public Integer getRouteId() { return routeId; }
    public void setRouteId(Integer routeId) { this.routeId = routeId; }
    public LocalDate getRouteDate() { return routeDate; }
    public void setRouteDate(LocalDate routeDate) { this.routeDate = routeDate; }
    public AppUser getSalesOfficer() { return salesOfficer; }
    public void setSalesOfficer(AppUser salesOfficer) { this.salesOfficer = salesOfficer; }
}