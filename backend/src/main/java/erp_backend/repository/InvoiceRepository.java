package erp_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import erp_backend.entity.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {}