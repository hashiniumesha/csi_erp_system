package erp_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import erp_backend.entity.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Integer> {
    Optional<AppUser> findByUsername(String username);
}