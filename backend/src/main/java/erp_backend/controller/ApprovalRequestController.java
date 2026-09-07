package erp_backend.controller;

import erp_backend.entity.ApprovalRequest;
import erp_backend.repository.ApprovalRequestRepository;
import erp_backend.service.ApprovalRequestService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/approval-requests")
public class ApprovalRequestController {

    @Autowired private ApprovalRequestService approvalRequestService;
    @Autowired private ApprovalRequestRepository approvalRequestRepository;

    // Safe projection — deliberately doesn't expose the requestedBy/reviewedBy
    // AppUser entities directly, the same reason UserController never returns
    // raw AppUser (it would leak passwordHash).
    public static class RequestSummary {
        public Integer requestId;
        public String requestType;
        public String entityType;
        public Integer entityId;
        public String payloadJson;
        public String requestedByName;
        public String status;
        public String adminNote;
        public String reviewedByName;
        public String requestedAt;
        public String reviewedAt;
    }

    public static class SubmitRequest {
        public String requestType;
        public String entityType;
        public Integer entityId;
        public Map<String, Object> payload;
        public Integer requestedByUserId;
    }

    public static class ReviewRequest {
        public Integer reviewedByUserId;
        public String adminNote;
    }

    // Inventory Manager submits here instead of calling RawMaterialController
    // directly - see RoleAccessFilter, which now only allows Admin on the
    // raw POST/PUT/DELETE /api/raw-materials endpoints.
    @PostMapping
    public RequestSummary submit(@RequestBody SubmitRequest request) {
        ApprovalRequest saved = approvalRequestService.submit(
                ApprovalRequest.RequestType.valueOf(request.requestType),
                request.entityType,
                request.entityId,
                request.payload != null ? new JSONObject(request.payload) : new JSONObject(),
                request.requestedByUserId
        );
        return toSummary(saved);
    }

    // Admin's queue of requests waiting for a decision.
    @GetMapping("/pending")
    public List<RequestSummary> listPending() {
        return approvalRequestRepository.findByStatusOrderByRequestedAtDesc(ApprovalRequest.Status.Pending)
                .stream().map(ApprovalRequestController::toSummary).toList();
    }

    // A user's own request history, so they can see Pending/Approved/Rejected
    // status on things they've asked for.
    @GetMapping("/mine")
    public List<RequestSummary> listMine(@RequestParam Integer userId) {
        return approvalRequestRepository.findByRequestedBy_UserIdOrderByRequestedAtDesc(userId)
                .stream().map(ApprovalRequestController::toSummary).toList();
    }

    @PostMapping("/{id}/approve")
    public RequestSummary approve(@PathVariable Integer id, @RequestBody ReviewRequest request) {
        return toSummary(approvalRequestService.approve(id, request.reviewedByUserId));
    }

    @PostMapping("/{id}/reject")
    public RequestSummary reject(@PathVariable Integer id, @RequestBody ReviewRequest request) {
        return toSummary(approvalRequestService.reject(id, request.reviewedByUserId, request.adminNote));
    }

    private static RequestSummary toSummary(ApprovalRequest r) {
        RequestSummary s = new RequestSummary();
        s.requestId = r.getRequestId();
        s.requestType = r.getRequestType().name();
        s.entityType = r.getEntityType();
        s.entityId = r.getEntityId();
        s.payloadJson = r.getPayloadJson();
        s.requestedByName = r.getRequestedBy() != null ? r.getRequestedBy().getFullName() : null;
        s.status = r.getStatus().name();
        s.adminNote = r.getAdminNote();
        s.reviewedByName = r.getReviewedBy() != null ? r.getReviewedBy().getFullName() : null;
        s.requestedAt = r.getRequestedAt() != null ? r.getRequestedAt().toString() : null;
        s.reviewedAt = r.getReviewedAt() != null ? r.getReviewedAt().toString() : null;
        return s;
    }
}
