package erp_backend.repository;

import erp_backend.entity.GRN;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GRNRepository extends JpaRepository<GRN, Integer> {
    // Each GRN row is one raw-material batch, carrying that batch's own
    // UnitCost and DateReceived — this is what lets a specific delivery be
    // told apart from another of the same material at a different price/date.
    List<GRN> findByRawMaterial_RawMaterialId(Integer rawMaterialId);
}