package erp_backend.controller;

import erp_backend.entity.*;
import erp_backend.repository.*;
import erp_backend.service.InventoryService;
import erp_backend.util.ExpiryCalculator;
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
    @Autowired private ExpiryBatchRepository expiryBatchRepository;

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
        // Only meaningful for a finished-product IN movement - the date this
        // batch was actually produced. Defaults to today when omitted.
        public String productionDate;
        public Double unitCost;
    }

    public static class DamagedRequest {
        public Integer productId;
        public Double quantity;
        public String cause;
        public String stage;
    }

    public static class ExpiryRequest {
        public Integer productId;
        public String productionDate;
        public Double quantity;
        public Double unitCost;
    }

    // What the Inventory / QC "expiry status" screens actually need per
    // batch - the raw ExpiryBatch entity plus the product's own name/unit
    // and the computed status, so the frontend never has to re-derive
    // expiry math itself.
    public static class ExpiryBatchView {
        public Integer batchId;
        public Integer productId;
        public String productName;
        public String category;
        public String productionDate;
        public String expiryDate;
        public Double quantity;
        public Double unitCost;
        public long daysRemaining;
        public String status; // EXPIRED, EXPIRING_SOON, OK

        static ExpiryBatchView from(ExpiryBatch b) {
            ExpiryBatchView v = new ExpiryBatchView();
            v.batchId = b.getBatchId();
            v.productId = b.getProduct() != null ? b.getProduct().getProductId() : null;
            v.productName = b.getProduct() != null ? b.getProduct().getName() : "?";
            v.category = b.getProduct() != null ? b.getProduct().getCategory() : null;
            v.productionDate = b.getProductionDate() != null ? b.getProductionDate().toString() : null;
            v.expiryDate = b.getExpiryDate() != null ? b.getExpiryDate().toString() : null;
            v.quantity = b.getQuantity();
            v.unitCost = b.getUnitCost();
            v.daysRemaining = ExpiryCalculator.daysRemaining(b.getExpiryDate());
            v.status = ExpiryCalculator.statusOf(b.getExpiryDate()).name();
            return v;
        }
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
        LocalDate productionDate = request.productionDate != null && !request.productionDate.isBlank()
                ? LocalDate.parse(request.productionDate) : null;
        return inventoryService.recordFinishedProductMovement(product,
                FinishedProductStockMovement.MovementType.valueOf(request.movementType),
                request.quantity, request.referenceType, request.referenceId, productionDate, request.unitCost);
    }

    @PostMapping("/damaged")
    public DamagedProduct recordDamaged(@RequestBody DamagedRequest request) {
        FinishedProduct product = finishedProductRepository.findById(request.productId)
                .orElseThrow(() -> new RuntimeException("Finished product not found"));
        return inventoryService.recordDamagedProduct(product, request.quantity, request.cause,
                DamagedProduct.Stage.valueOf(request.stage));
    }

    @PostMapping("/expiry")
    public ExpiryBatchView recordExpiry(@RequestBody ExpiryRequest request) {
        FinishedProduct product = finishedProductRepository.findById(request.productId)
                .orElseThrow(() -> new RuntimeException("Finished product not found"));
        LocalDate productionDate = request.productionDate != null && !request.productionDate.isBlank()
                ? LocalDate.parse(request.productionDate) : LocalDate.now();
        return ExpiryBatchView.from(inventoryService.recordExpiryBatch(product, productionDate, request.quantity, request.unitCost));
    }

    // Batch history for one specific finished product — every production
    // batch, each carrying its own unit cost and expiry date, instead of
    // the single pooled CurrentStock number on FinishedProduct itself.
    @GetMapping("/finished-products/{id}/batches")
    public List<ExpiryBatchView> listBatchesForProduct(@PathVariable Integer id) {
        return expiryBatchRepository.findByProduct_ProductIdOrderByExpiryDateAsc(id).stream()
                .map(ExpiryBatchView::from).toList();
    }

    // Every finished-product batch across the whole company, soonest expiry
    // first — what the Inventory "Expiry Tracking" screen is built on:
    // Product Name, Batch Number, Production Date, Expiry Date, Available
    // Quantity, and Expiry Status, per batch rather than one figure for the
    // whole product.
    @GetMapping("/expiry-batches")
    public List<ExpiryBatchView> listAllExpiryBatches() {
        return expiryBatchRepository.findAllByOrderByExpiryDateAsc().stream()
                .map(ExpiryBatchView::from).toList();
    }
}
