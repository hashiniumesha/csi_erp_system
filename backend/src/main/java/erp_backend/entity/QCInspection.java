package erp_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "QCInspection")
public class QCInspection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "InspectionID")
    private Integer inspectionId;

    @OneToOne
    @JoinColumn(name = "GRNID", nullable = false, unique = true)
    private GRN grn;

    @ManyToOne
    @JoinColumn(name = "QCOfficerID", nullable = false)
    private AppUser qcOfficer;

    @Column(name = "InspectionDate", nullable = false)
    private LocalDate inspectionDate;

    @Column(name = "Parameters")
    private String parameters;

    @Enumerated(EnumType.STRING)
    @Column(name = "Decision", nullable = false)
    private Decision decision;

    @Column(name = "Reason")
    private String reason;

    public enum Decision { Approved, Rejected }

    public Integer getInspectionId() { return inspectionId; }
    public void setInspectionId(Integer inspectionId) { this.inspectionId = inspectionId; }
    public GRN getGrn() { return grn; }
    public void setGrn(GRN grn) { this.grn = grn; }
    public AppUser getQcOfficer() { return qcOfficer; }
    public void setQcOfficer(AppUser qcOfficer) { this.qcOfficer = qcOfficer; }
    public LocalDate getInspectionDate() { return inspectionDate; }
    public void setInspectionDate(LocalDate inspectionDate) { this.inspectionDate = inspectionDate; }
    public String getParameters() { return parameters; }
    public void setParameters(String parameters) { this.parameters = parameters; }
    public Decision getDecision() { return decision; }
    public void setDecision(Decision decision) { this.decision = decision; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
