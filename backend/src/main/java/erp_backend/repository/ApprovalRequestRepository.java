package erp_backend.repository;

import erp_backend.entity.ApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Integer> {
    List<ApprovalRequest> findByStatusOrderByRequestedAtDesc(ApprovalRequest.Status status);
    List<ApprovalRequest> findByRequestedBy_UserIdOrderByRequestedAtDesc(Integer userId);
}
