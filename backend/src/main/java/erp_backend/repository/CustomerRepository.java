package erp_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import erp_backend.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {}