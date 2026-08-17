package erp_backend.repository;

import erp_backend.entity.FinishedProduct;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FinishedProductRepository extends JpaRepository<FinishedProduct, Integer> {}