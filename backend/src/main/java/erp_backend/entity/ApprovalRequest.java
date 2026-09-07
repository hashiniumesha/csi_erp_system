package erp_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A generic admin approval/request record. EntityType names which table the
 * request concerns (e.g. "RawMaterial") rather than this being a dedicated
 * "RawMaterialRequest" table — the same mechanism can cover other modules
 * later without a new table per module. PayloadJson holds the proposed
 * field values as a JSON string (this app already uses org.json elsewhere,
 * so a generic text payload was simpler than a column-per-possible-field
 * design that would need to anticipate every future entity's shape).
 */
@Entity
@Table(name = "ApprovalRequest")
public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RequestID")
    private Integer requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "RequestType", nullable = false)
    private RequestType requestType;

    @Column(name = "EntityType", nullable = false)
    private String entityType;

    @Column(name = "EntityID")
    private Integer entityId;

    @Column(name = "PayloadJson", columnDefinition = "TEXT")
    private String payloadJson;

    @ManyToOne
    @JoinColumn(name = "RequestedByUserID", nullable = false)
    private AppUser requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    private Status status = Status.Pending;

    @ManyToOne
    @JoinColumn(name = "ReviewedByUserID")
    private AppUser reviewedBy;

    @Column(name = "AdminNote")
    private String adminNote;

    @Column(name = "RequestedAt", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "ReviewedAt")
    private LocalDateTime reviewedAt;

    public enum RequestType { CREATE, UPDATE, DELETE }
    public enum Status { Pending, Approved, Rejected }

    public Integer getRequestId() { return requestId; }
    public void setRequestId(Integer requestId) { this.requestId = requestId; }
    public RequestType getRequestType() { return requestType; }
    public void setRequestType(RequestType requestType) { this.requestType = requestType; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public Integer getEntityId() { return entityId; }
    public void setEntityId(Integer entityId) { this.entityId = entityId; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public AppUser getRequestedBy() { return requestedBy; }
    public void setRequestedBy(AppUser requestedBy) { this.requestedBy = requestedBy; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public AppUser getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(AppUser reviewedBy) { this.reviewedBy = reviewedBy; }
    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
