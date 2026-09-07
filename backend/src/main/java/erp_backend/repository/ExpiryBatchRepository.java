package erp_backend.repository;

import erp_backend.entity.ExpiryBatch;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpiryBatchRepository extends JpaRepository<ExpiryBatch, Integer> {
    List<ExpiryBatch> findByProduct_ProductId(Integer productId);

    // Soonest-expiring batch first - what FIFO depletion and the expiry
    // dashboard both need.
    List<ExpiryBatch> findByProduct_ProductIdOrderByExpiryDateAsc(Integer productId);

    List<ExpiryBatch> findAllByOrderByExpiryDateAsc();
}