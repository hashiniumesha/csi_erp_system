package erp_backend.service;

import erp_backend.entity.*;
import erp_backend.repository.*;
import erp_backend.util.ExpiryCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class InventoryService {

    @Autowired private FinishedProductRepository finishedProductRepository;
    @Autowired private RawMaterialRepository rawMaterialRepository;
    @Autowired private RawMaterialStockMovementRepository rawMaterialMovementRepository;
    @Autowired private FinishedProductStockMovementRepository finishedProductMovementRepository;
    @Autowired private DamagedProductRepository damagedProductRepository;
    @Autowired private ExpiryBatchRepository expiryBatchRepository;

    public FinishedProduct createFinishedProduct(String name, String category, String unitOfMeasure) {
        FinishedProduct product = new FinishedProduct();
        product.setName(name);
        product.setCategory(category);
        product.setUnitOfMeasure(unitOfMeasure);
        product.setCurrentStock(0.0);
        return finishedProductRepository.save(product);
    }

    public RawMaterialStockMovement recordRawMaterialMovement(RawMaterial rawMaterial, RawMaterialStockMovement.MovementType type,
                                                                Double quantity, String referenceType, Integer referenceId) {
        RawMaterialStockMovement movement = new RawMaterialStockMovement();
        movement.setRawMaterial(rawMaterial);
        movement.setMovementType(type);
        movement.setQuantity(quantity);
        movement.setMovementDate(LocalDate.now());
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        rawMaterialMovementRepository.save(movement);

        double delta = (type == RawMaterialStockMovement.MovementType.IN) ? quantity : -quantity;
        rawMaterial.setCurrentStock(rawMaterial.getCurrentStock() + delta);
        rawMaterialRepository.save(rawMaterial);

        return movement;
    }

    // Overload kept for callers that don't care about expiry (e.g. the raw
    // material path, which doesn't batch-track expiry - see class comment
    // on ExpiryBatch). Finished-product IN movements should go through the
    // productionDate overload below instead so each new batch of stock gets
    // its own expiry-tracked batch record.
    public FinishedProductStockMovement recordFinishedProductMovement(FinishedProduct product, FinishedProductStockMovement.MovementType type,
                                                                        Double quantity, String referenceType, Integer referenceId) {
        return recordFinishedProductMovement(product, type, quantity, referenceType, referenceId, null, null);
    }

    public FinishedProductStockMovement recordFinishedProductMovement(FinishedProduct product, FinishedProductStockMovement.MovementType type,
                                                                        Double quantity, String referenceType, Integer referenceId,
                                                                        LocalDate productionDate, Double unitCost) {
        FinishedProductStockMovement movement = new FinishedProductStockMovement();
        movement.setProduct(product);
        movement.setMovementType(type);
        movement.setQuantity(quantity);
        movement.setMovementDate(LocalDate.now());
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        finishedProductMovementRepository.save(movement);

        double delta = (type == FinishedProductStockMovement.MovementType.IN) ? quantity : -quantity;
        product.setCurrentStock(product.getCurrentStock() + delta);
        finishedProductRepository.save(product);

        if (type == FinishedProductStockMovement.MovementType.IN) {
            // New stock coming in is new production - it gets its own batch,
            // production date, and auto-calculated expiry date, distinct
            // from every other batch of the same product.
            recordExpiryBatch(product, productionDate != null ? productionDate : LocalDate.now(), quantity, unitCost);
        } else {
            // Stock leaving reduces the batches actually holding it,
            // soonest-expiring first, so "available quantity" per batch
            // stays honest.
            depleteExpiryBatchesFifo(product, quantity);
        }

        return movement;
    }

    public DamagedProduct recordDamagedProduct(FinishedProduct product, Double quantity, String cause, DamagedProduct.Stage stage) {
        DamagedProduct damaged = new DamagedProduct();
        damaged.setProduct(product);
        damaged.setQuantity(quantity);
        damaged.setCause(cause);
        damaged.setStage(stage);
        damaged.setDamageDate(LocalDate.now());
        damagedProductRepository.save(damaged);

        product.setCurrentStock(product.getCurrentStock() - quantity);
        finishedProductRepository.save(product);

        // Damaged stock is stock leaving too - deplete it from the batches
        // it actually came from, same as a Stock Out movement.
        depleteExpiryBatchesFifo(product, quantity);

        return damaged;
    }

    // productionDate is the date this batch was actually produced / put
    // into stock; expiryDate is always derived from it via ExpiryCalculator
    // (Milk Packets -> +2 months, Yogurt -> +3 months, everything else ->
    // +1 year) rather than being typed in, so it can never be miscalculated
    // by hand.
    public ExpiryBatch recordExpiryBatch(FinishedProduct product, LocalDate productionDate, Double quantity, Double unitCost) {
        LocalDate effectiveProductionDate = productionDate != null ? productionDate : LocalDate.now();
        ExpiryBatch batch = new ExpiryBatch();
        batch.setProduct(product);
        batch.setProductionDate(effectiveProductionDate);
        batch.setExpiryDate(ExpiryCalculator.calculateExpiryDate(product.getCategory(), product.getName(), effectiveProductionDate));
        batch.setQuantity(quantity);
        batch.setUnitCost(unitCost);
        return expiryBatchRepository.save(batch);
    }

    // FIFO = the batch closest to expiry is used up first, exactly like a
    // real warehouse would rotate stock. Walks batches oldest-expiry-first,
    // subtracting from each until the requested quantity is used up or
    // batches run out. A batch is deleted once it reaches zero so "how much
    // of this batch is left" never has to filter out empty rows.
    public void depleteExpiryBatchesFifo(FinishedProduct product, Double quantity) {
        if (quantity == null || quantity <= 0) return;
        double remaining = quantity;
        List<ExpiryBatch> batches = expiryBatchRepository.findByProduct_ProductIdOrderByExpiryDateAsc(product.getProductId());
        for (ExpiryBatch batch : batches) {
            if (remaining <= 0) break;
            double available = batch.getQuantity() != null ? batch.getQuantity() : 0.0;
            if (available <= 0) continue;
            double take = Math.min(available, remaining);
            double left = available - take;
            remaining -= take;
            if (left <= 0.0001) {
                expiryBatchRepository.delete(batch);
            } else {
                batch.setQuantity(left);
                expiryBatchRepository.save(batch);
            }
        }
        // If `remaining` is still > 0 here, more stock left than any batch
        // on record covers (e.g. stock predates batch tracking being wired
        // in). CurrentStock on the product itself is still the source of
        // truth for total stock - batches are only for expiry tracking - so
        // this isn't treated as an error.
    }
}
