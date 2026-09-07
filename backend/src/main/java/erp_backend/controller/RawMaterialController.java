package erp_backend.controller;

import erp_backend.entity.RawMaterial;
import erp_backend.repository.RawMaterialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/raw-materials")
public class RawMaterialController {

    @Autowired private RawMaterialRepository rawMaterialRepository;

    public static class RawMaterialRequest {
        public String name;
        public Double reorderLevel;
    }

    @PostMapping
    public RawMaterial create(@RequestBody RawMaterialRequest request) {
        RawMaterial material = new RawMaterial();
        material.setName(request.name);
        material.setCurrentStock(0.0);
        material.setReorderLevel(request.reorderLevel != null ? request.reorderLevel : 0.0);
        return rawMaterialRepository.save(material);
    }

    @PutMapping("/{id}")
    public RawMaterial update(@PathVariable Integer id, @RequestBody RawMaterialRequest request) {
        RawMaterial material = rawMaterialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Raw material not found"));
        material.setName(request.name);
        material.setReorderLevel(request.reorderLevel != null ? request.reorderLevel : material.getReorderLevel());
        return rawMaterialRepository.save(material);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        try {
            rawMaterialRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("This raw material has GRN or stock movement records and can't be deleted.");
        }
    }
}