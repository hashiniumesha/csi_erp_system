package erp_backend.controller;

import erp_backend.entity.*;
import erp_backend.repository.*;
import erp_backend.service.QCService;
import erp_backend.util.ExpiryCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class QCController {

    @Autowired private QCService qcService;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private RawMaterialRepository rawMaterialRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private GRNRepository grnRepository;

    // List endpoints — used to populate dropdown/select fields on the frontend
    // instead of requiring users to type in raw numeric IDs.
    @GetMapping("/suppliers")
    public List<Supplier> listSuppliers() {
        return supplierRepository.findAll();
    }

    @GetMapping("/raw-materials")
    public List<RawMaterial> listRawMaterials() {
        return rawMaterialRepository.findAll();
    }

    // A GRN row doubles as the raw-material "batch" record: each one already
    // carries the price (UnitCost) and date (DateReceived) for that
    // delivery, which is what distinguishes one batch from another once
    // stock is pooled. DateReceived also stands in as this batch's
    // production/stock-in date for expiry purposes — raw materials don't
    // get a separate batch table the way finished products do (see
    // ExpiryBatch), so expiry here is computed on the fly from the same
    // Milk/Yogurt/standard rule, keyed off the raw material's own name.
    public static class GRNBatchView {
        public Integer grnId;
        public Supplier supplier;
        public RawMaterial rawMaterial;
        public Double quantityReceived;
        public Double unitCost;
        public String dateReceived;
        public String status;
        public String expiryDate;
        public long daysRemaining;
        public String expiryStatus; // EXPIRED, EXPIRING_SOON, OK

        static GRNBatchView from(GRN g) {
            GRNBatchView v = new GRNBatchView();
            v.grnId = g.getGrnId();
            v.supplier = g.getSupplier();
            v.rawMaterial = g.getRawMaterial();
            v.quantityReceived = g.getQuantityReceived();
            v.unitCost = g.getUnitCost();
            v.dateReceived = g.getDateReceived() != null ? g.getDateReceived().toString() : null;
            v.status = g.getStatus() != null ? g.getStatus().name() : null;
            String materialName = g.getRawMaterial() != null ? g.getRawMaterial().getName() : null;
            LocalDate expiry = ExpiryCalculator.calculateExpiryDate(null, materialName, g.getDateReceived());
            v.expiryDate = expiry != null ? expiry.toString() : null;
            v.daysRemaining = ExpiryCalculator.daysRemaining(expiry);
            v.expiryStatus = ExpiryCalculator.statusOf(expiry).name();
            return v;
        }
    }

    @GetMapping("/qc/grns")
    public List<GRNBatchView> listGrns() {
        return grnRepository.findAll().stream().map(GRNBatchView::from).toList();
    }

    // Batch history for one specific raw material — every delivery of this
    // material, each carrying its own price, date, and computed expiry.
    @GetMapping("/raw-materials/{id}/batches")
    public List<GRNBatchView> listBatchesForRawMaterial(@PathVariable Integer id) {
        return grnRepository.findByRawMaterial_RawMaterialId(id).stream().map(GRNBatchView::from).toList();
    }

    public static class GRNRequest {
        public Integer supplierId;
        public Integer rawMaterialId;
        public Double quantityReceived;
        public Double unitCost;
    }

    public static class QCDecisionRequest {
        public Integer qcOfficerId;
        public String parameters;
        public String reason;
    }

    @PostMapping("/grn")
    public GRN submitGRN(@RequestBody GRNRequest request) {
        Supplier supplier = supplierRepository.findById(request.supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
        RawMaterial rawMaterial = rawMaterialRepository.findById(request.rawMaterialId)
                .orElseThrow(() -> new RuntimeException("Raw Material not found"));
        return qcService.submitForInspection(supplier, rawMaterial, request.quantityReceived, request.unitCost, LocalDate.now());
    }

    @PostMapping("/qc/{grnId}/approve")
    public String approve(@PathVariable Integer grnId, @RequestBody QCDecisionRequest request) {
        AppUser qcOfficer = appUserRepository.findById(request.qcOfficerId)
                .orElseThrow(() -> new RuntimeException("QC Officer not found"));
        qcService.approve(grnId, qcOfficer, request.parameters);
        return "Batch approved and released to inventory";
    }

    @PostMapping("/qc/{grnId}/reject")
    public String reject(@PathVariable Integer grnId, @RequestBody QCDecisionRequest request) {
        AppUser qcOfficer = appUserRepository.findById(request.qcOfficerId)
                .orElseThrow(() -> new RuntimeException("QC Officer not found"));
        qcService.reject(grnId, qcOfficer, request.parameters, request.reason);
        return "Batch rejected";
    }
}
