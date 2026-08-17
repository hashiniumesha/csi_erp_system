package erp_backend.repository;

import erp_backend.entity.DamagedProduct;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DamagedProductRepository extends JpaRepository<DamagedProduct, Integer> {}
