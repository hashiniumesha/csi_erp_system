package erp_backend.service;

import erp_backend.entity.*;
import erp_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
public class QCService {

    @Autowired private GRNRepository grnRepository;
    @Autowired private QCInspectionRepository qcInspectionRepository;
    @Autowired private RawMaterialRepository rawMaterialRepository;

    public GRN submitForInspection(Supplier supplier, RawMaterial rawMaterial, Double quantityReceived, Double unitCost, LocalDate dateReceived) {
        GRN grn = new GRN();
        grn.setSupplier(supplier);
        grn.setRawMaterial(rawMaterial);
        grn.setQuantityReceived(quantityReceived);
        grn.setUnitCost(unitCost);
        grn.setDateReceived(dateReceived);
        grn.setStatus(GRN.Status.Pending);
        return grnRepository.save(grn);
    }

    public void approve(Integer grnId, AppUser qcOfficer, String parameters) {
        GRN grn = grnRepository.findById(grnId).orElseThrow(() -> new RuntimeException("GRN not found"));

        QCInspection inspection = new QCInspection();
        inspection.setGrn(grn);
        inspection.setQcOfficer(qcOfficer);
        inspection.setInspectionDate(LocalDate.now());
        inspection.setParameters(parameters);
        inspection.setDecision(QCInspection.Decision.Approved);
        qcInspectionRepository.save(inspection);

        grn.setStatus(GRN.Status.Approved);
        grnRepository.save(grn);

        RawMaterial rawMaterial = grn.getRawMaterial();
        rawMaterial.setCurrentStock(rawMaterial.getCurrentStock() + grn.getQuantityReceived());
        rawMaterialRepository.save(rawMaterial);
    }

    public void reject(Integer grnId, AppUser qcOfficer, String parameters, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new RuntimeException("A reason is required to reject a batch");
        }
        GRN grn = grnRepository.findById(grnId).orElseThrow(() -> new RuntimeException("GRN not found"));

        QCInspection inspection = new QCInspection();
        inspection.setGrn(grn);
        inspection.setQcOfficer(qcOfficer);
        inspection.setInspectionDate(LocalDate.now());
        inspection.setParameters(parameters);
        inspection.setDecision(QCInspection.Decision.Rejected);
        inspection.setReason(reason);
        qcInspectionRepository.save(inspection);

        grn.setStatus(GRN.Status.Rejected);
        grnRepository.save(grn);
    }
}