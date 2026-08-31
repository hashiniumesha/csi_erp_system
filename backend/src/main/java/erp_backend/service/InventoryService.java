package erp_backend.service;

import erp_backend.entity.*;
import erp_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

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

    public FinishedProductStockMovement recordFinishedProductMovement(FinishedProduct product, FinishedProductStockMovement.MovementType type,
                                                                        Double quantity, String referenceType, Integer referenceId) {
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

        return damaged;
    }

    public ExpiryBatch recordExpiryBatch(FinishedProduct product, LocalDate expiryDate, Double quantity, Double unitCost) {
        ExpiryBatch batch = new ExpiryBatch();
        batch.setProduct(product);
        batch.setExpiryDate(expiryDate);
        batch.setQuantity(quantity);
        batch.setUnitCost(unitCost);
        return expiryBatchRepository.save(batch);
    }
}