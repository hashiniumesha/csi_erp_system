package erp_backend.repository;

import erp_backend.entity.RawMaterialStockMovement;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RawMaterialStockMovementRepository extends JpaRepository<RawMaterialStockMovement, Integer> {}