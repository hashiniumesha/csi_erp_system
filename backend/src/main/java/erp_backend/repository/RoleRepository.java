package erp_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import erp_backend.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Integer> {
}