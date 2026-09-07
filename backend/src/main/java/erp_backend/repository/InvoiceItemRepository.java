package erp_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import erp_backend.entity.InvoiceItem;

import java.util.List;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Integer> {
    List<InvoiceItem> findByInvoice_InvoiceId(Integer invoiceId);
}
