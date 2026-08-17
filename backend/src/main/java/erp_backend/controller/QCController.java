package erp_backend.controller;

import erp_backend.entity.*;
import erp_backend.repository.*;
import erp_backend.service.QCService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api")
public class QCController {

    @Autowired private QCService qcService;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private RawMaterialRepository rawMaterialRepository;
    @Autowired private AppUserRepository appUserRepository;

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