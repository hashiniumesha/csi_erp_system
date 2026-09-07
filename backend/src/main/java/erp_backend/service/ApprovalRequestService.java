package erp_backend.service;

import erp_backend.entity.*;
import erp_backend.repository.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Applies an approved request's proposed change to the real entity. Only
 * RawMaterial is wired up today - a later request for a different module
 * (Inventory's finished products, say) adds one more case in applyChange()
 * rather than a new approval mechanism, since ApprovalRequest itself is
 * already generic across entity types.
 */
@Service
public class ApprovalRequestService {

    @Autowired private ApprovalRequestRepository approvalRequestRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private RawMaterialRepository rawMaterialRepository;

    public ApprovalRequest submit(ApprovalRequest.RequestType type, String entityType, Integer entityId,
                                   JSONObject payload, Integer requestedByUserId) {
        AppUser requestedBy = appUserRepository.findById(requestedByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ApprovalRequest request = new ApprovalRequest();
        request.setRequestType(type);
        request.setEntityType(entityType);
        request.setEntityId(entityId);
        request.setPayloadJson(payload != null ? payload.toString() : "{}");
        request.setRequestedBy(requestedBy);
        request.setStatus(ApprovalRequest.Status.Pending);
        request.setRequestedAt(LocalDateTime.now());
        return approvalRequestRepository.save(request);
    }

    public ApprovalRequest approve(Integer requestId, Integer reviewedByUserId) {
        ApprovalRequest request = getPendingOrThrow(requestId);
        AppUser reviewer = appUserRepository.findById(reviewedByUserId)
                .orElseThrow(() -> new RuntimeException("Reviewer not found"));

        applyChange(request);

        request.setStatus(ApprovalRequest.Status.Approved);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(LocalDateTime.now());
        return approvalRequestRepository.save(request);
    }

    public ApprovalRequest reject(Integer requestId, Integer reviewedByUserId, String adminNote) {
        ApprovalRequest request = getPendingOrThrow(requestId);
        AppUser reviewer = appUserRepository.findById(reviewedByUserId)
                .orElseThrow(() -> new RuntimeException("Reviewer not found"));

        request.setStatus(ApprovalRequest.Status.Rejected);
        request.setReviewedBy(reviewer);
        request.setAdminNote(adminNote);
        request.setReviewedAt(LocalDateTime.now());
        return approvalRequestRepository.save(request);
    }

    private ApprovalRequest getPendingOrThrow(Integer requestId) {
        ApprovalRequest request = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        if (request.getStatus() != ApprovalRequest.Status.Pending) {
            throw new RuntimeException("This request has already been reviewed.");
        }
        return request;
    }

    private void applyChange(ApprovalRequest request) {
        if (!"RawMaterial".equals(request.getEntityType())) {
            throw new RuntimeException("Approval for entity type '" + request.getEntityType() + "' isn't supported yet.");
        }

        JSONObject payload = new JSONObject(
                request.getPayloadJson() != null ? request.getPayloadJson() : "{}");

        switch (request.getRequestType()) {
            case CREATE -> {
                RawMaterial material = new RawMaterial();
                material.setName(payload.getString("name"));
                material.setCurrentStock(0.0);
                material.setReorderLevel(payload.optDouble("reorderLevel", 0.0));
                material.setUnitOfMeasure(payload.optString("unitOfMeasure", null));
                rawMaterialRepository.save(material);
            }
            case UPDATE -> {
                RawMaterial material = rawMaterialRepository.findById(request.getEntityId())
                        .orElseThrow(() -> new RuntimeException("Raw material not found"));
                material.setName(payload.getString("name"));
                material.setReorderLevel(payload.optDouble("reorderLevel", material.getReorderLevel()));
                if (payload.has("unitOfMeasure")) {
                    material.setUnitOfMeasure(payload.optString("unitOfMeasure", material.getUnitOfMeasure()));
                }
                rawMaterialRepository.save(material);
            }
            case DELETE -> rawMaterialRepository.deleteById(request.getEntityId());
        }
    }
}
