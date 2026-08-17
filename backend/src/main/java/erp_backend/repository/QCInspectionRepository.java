package erp_backend.repository;

import erp_backend.entity.QCInspection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QCInspectionRepository extends JpaRepository<QCInspection, Integer> {
}