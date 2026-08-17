package erp_backend.controller;

import erp_backend.entity.*;
import erp_backend.repository.*;
import erp_backend.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired private InventoryService inventoryService;
    @Autowired private FinishedProductRepository finishedProductRepository;
    @Autowired private RawMaterialRepository rawMaterialRepository;

    public static class FinishedProductRequest {
        public String name;
        public String category;
        public String unitOfMeasure;
    }

    public static class MovementRequest {
        public Integer itemId;
        public String movementType;
        public Double quantity;
        public String referenceType;
        public Integer referenceId;
    }

    public static class DamagedRequest {
        public Integer productId;
        public Double quantity;
        public String cause;
        public String stage;
    }

    public static class ExpiryRequest {
        public Integer productId;
        public String expiryDate;
        public Double quantity;
    }

    @PostMapping("/finished-product")
    public FinishedProduct createFinishedProduct(@RequestBody FinishedProductRequest request) {
        return inventoryService.createFinishedProduct(request.name, request.category, request.unitOfMeasure);
    }

    @GetMapping("/finished-products")
    public List<FinishedProduct> listFinishedProducts() {
        return finishedProductRepository.findAll();
    }

    @PostMapping("/raw-material-movement")
    public RawMaterialStockMovement recordRawMaterialMovement(@RequestBody MovementRequest request) {
        RawMaterial rawMaterial = rawMaterialRepository.findById(request.itemId)
                .orElseThrow(() -> new RuntimeException("Raw material not found"));
        return inventoryService.recordRawMaterialMovement(rawMaterial,
                RawMaterialStockMovement.MovementType.valueOf(request.movementType),
                request.quantity, request.referenceType, request.referenceId);
    }

    @PostMapping("/finished-product-movement")
    public FinishedProductStockMovement recordFinishedProductMovement(@RequestBody MovementRequest request) {
        FinishedProduct product = finishedProductRepository.findById(request.itemId)
                .orElseThrow(() -> new RuntimeException("Finished product not found"));
        return inventoryService.recordFinishedProductMovement(product,
                FinishedProductStockMovement.MovementType.valueOf(request.movementType),
                request.quantity, request.referenceType, request.referenceId);
    }

    @PostMapping("/damaged")
    public DamagedProduct recordDamaged(@RequestBody DamagedRequest request) {
        FinishedProduct product = finishedProductRepository.findById(request.productId)
                .orElseThrow(() -> new RuntimeException("Finished product not found"));
        return inventoryService.recordDamagedProduct(product, request.quantity, request.cause,
                DamagedProduct.Stage.valueOf(request.stage));
    }

    @PostMapping("/expiry")
    public ExpiryBatch recordExpiry(@RequestBody ExpiryRequest request) {
        FinishedProduct product = finishedProductRepository.findById(request.productId)
                .orElseThrow(() -> new RuntimeException("Finished product not found"));
        return inventoryService.recordExpiryBatch(product, LocalDate.parse(request.expiryDate), request.quantity);
    }
}