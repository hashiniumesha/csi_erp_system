package erp_backend.controller;

import erp_backend.entity.DamagedProduct;
import erp_backend.repository.DamagedProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Read-only view of damaged-product records. Deliberately a separate path
 * from /api/inventory (Inventory Manager-only) since this is scoped to
 * Admin + QC Officer only - QC reviews damage/wastage patterns even though
 * Inventory Manager is who records the damage in the first place.
 */
@RestController
@RequestMapping("/api/damaged-products")
public class DamagedProductController {

    @Autowired private DamagedProductRepository damagedProductRepository;

    public static class DamagedProductSummary {
        public Integer damageId;
        public String productName;
        public Double quantity;
        public String cause;
        public String stage;
        public String damageDate;
    }

    @GetMapping
    public List<DamagedProductSummary> listAll() {
        return damagedProductRepository.findAll().stream().map(d -> {
            DamagedProductSummary s = new DamagedProductSummary();
            s.damageId = d.getDamageId();
            s.productName = d.getProduct() != null ? d.getProduct().getName() : "?";
            s.quantity = d.getQuantity();
            s.cause = d.getCause();
            s.stage = d.getStage() != null ? d.getStage().name() : null;
            s.damageDate = d.getDamageDate() != null ? d.getDamageDate().toString() : null;
            return s;
        }).toList();
    }
}
