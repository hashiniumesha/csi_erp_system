package erp_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import erp_backend.entity.DeliveryRoute;

public interface DeliveryRouteRepository extends JpaRepository<DeliveryRoute, Integer> {}