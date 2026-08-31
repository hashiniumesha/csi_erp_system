package com.csi.erpfrontend;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Production / QC module.
 *
 * Every reference field (Supplier, Raw Material, QC Officer, GRN) is now a
 * ComboBox populated from the backend's list endpoints, instead of a
 * TextField the user had to type a raw numeric ID into. Numeric fields are
 * validated client-side before anything is sent to the server, and every
 * failure path shows a plain-language message instead of a raw response body.
 */
public class QCView {

    // Generic (id, label) pair used to back every dropdown in this screen.
    private record Option(int id, String label) {
        @Override public String toString() { return label; }
    }

    public static Node build() {
        Label title = new Label("Production / QC");
        title.getStyleClass().add("page-title");

        VBox grnCard = buildGrnCard();
        VBox decisionCard = buildDecisionCard();

        VBox layout = new VBox(20, title, grnCard, decisionCard);
        layout.setPadding(new Insets(28));

        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("page-bg");
        return scrollPane;
    }

    private static VBox buildGrnCard() {
        Label sectionTitle = new Label("Submit GRN (Goods Received Note)");
        sectionTitle.getStyleClass().add("section-title");

        ComboBox<Option> supplierBox = new ComboBox<>();
        supplierBox.setPromptText("Select supplier");
        supplierBox.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Option> rawMaterialBox = new ComboBox<>();
        rawMaterialBox.setPromptText("Select raw material");
        rawMaterialBox.setMaxWidth(Double.MAX_VALUE);

        try {
            for (Object o : ApiClient.getArray("/api/suppliers")) {
                JSONObject s = (JSONObject) o;
                supplierBox.getItems().add(new Option(s.getInt("supplierId"), s.getString("name")));
            }
            for (Object o : ApiClient.getArray("/api/raw-materials")) {
                JSONObject r = (JSONObject) o;
                rawMaterialBox.getItems().add(new Option(r.getInt("rawMaterialId"), r.getString("name")));
            }
        } catch (ApiClient.ApiException e) {
            supplierBox.setPromptText("Couldn't load suppliers");
            rawMaterialBox.setPromptText("Couldn't load raw materials");
        }

        TextField quantityField = new TextField();
        quantityField.setPromptText("e.g. 100");

        TextField unitCostField = new TextField();
        unitCostField.setPromptText("Price per unit, e.g. 250.00");

        Button submitButton = new Button("Submit GRN");
        submitButton.getStyleClass().add("button-primary");

        Label statusLabel = newHiddenStatusLabel();

        submitButton.setOnAction(e -> {
            if (supplierBox.getValue() == null) { showError(statusLabel, "Select a supplier."); return; }
            if (rawMaterialBox.getValue() == null) { showError(statusLabel, "Select a raw material."); return; }

            Double quantity = parsePositiveDouble(quantityField.getText());
            if (quantity == null) { showError(statusLabel, "Enter a valid quantity greater than 0."); return; }

            Double unitCost = parsePositiveDouble(unitCostField.getText());
            if (unitCost == null) { showError(statusLabel, "Enter a valid unit cost greater than 0."); return; }

            try {
                JSONObject body = new JSONObject();
                body.put("supplierId", supplierBox.getValue().id());
                body.put("rawMaterialId", rawMaterialBox.getValue().id());
                body.put("quantityReceived", quantity);
                body.put("unitCost", unitCost);

                JSONObject response = ApiClient.post("/api/grn", body);
                showSuccess(statusLabel, "GRN submitted — ID " + response.getInt("grnId") +
                        ", status " + response.getString("status") + ".");
                quantityField.clear();
                unitCostField.clear();
            } catch (ApiClient.ApiException ex) {
                showError(statusLabel, ex.getMessage());
            }
        });

        VBox card = new VBox(10,
                sectionTitle,
                labeled("Supplier", supplierBox),
                labeled("Raw material", rawMaterialBox),
                labeled("Quantity received", quantityField),
                labeled("Unit cost (price for this batch)", unitCostField),
                submitButton,
                statusLabel
        );
        card.getStyleClass().add("card");
        return card;
    }

    private static VBox buildDecisionCard() {
        Label sectionTitle = new Label("QC Decision (Approve / Reject)");
        sectionTitle.getStyleClass().add("section-title");

        ComboBox<Option> grnBox = new ComboBox<>();
        grnBox.setPromptText("Select a pending GRN");
        grnBox.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Option> officerBox = new ComboBox<>();
        officerBox.setPromptText("Select QC officer");
        officerBox.setMaxWidth(Double.MAX_VALUE);

        try {
            for (Object o : ApiClient.getArray("/api/qc/grns")) {
                JSONObject g = (JSONObject) o;
                if (!"Pending".equals(g.optString("status"))) continue;
                String supplierName = g.optJSONObject("supplier") != null ? g.getJSONObject("supplier").optString("name", "?") : "?";
                String materialName = g.optJSONObject("rawMaterial") != null ? g.getJSONObject("rawMaterial").optString("name", "?") : "?";
                String label = "GRN #" + g.getInt("grnId") + " — " + materialName + " (" + g.optDouble("quantityReceived") +
                        ") from " + supplierName + " — " + g.optString("dateReceived");
                grnBox.getItems().add(new Option(g.getInt("grnId"), label));
            }
            for (Object o : ApiClient.getArray("/api/users")) {
                JSONObject u = (JSONObject) o;
                String role = u.optString("roleName", "");
                if (!"QC Officer".equals(role) && !"Admin".equals(role)) continue;
                officerBox.getItems().add(new Option(u.getInt("userId"), u.getString("fullName") + " (" + role + ")"));
            }
        } catch (ApiClient.ApiException e) {
            grnBox.setPromptText("Couldn't load pending GRNs");
            officerBox.setPromptText("Couldn't load QC officers");
        }

        TextField parametersField = new TextField();
        parametersField.setPromptText("e.g. Sugar concentration and colour within spec");

        TextField reasonField = new TextField();
        reasonField.setPromptText("Required only when rejecting");

        Button approveButton = new Button("Approve");
        approveButton.getStyleClass().add("button-primary");

        Button rejectButton = new Button("Reject");
        rejectButton.getStyleClass().add("button-secondary");

        Label statusLabel = newHiddenStatusLabel();

        approveButton.setOnAction(e -> submitDecision(true, grnBox, officerBox, parametersField, reasonField, statusLabel));
        rejectButton.setOnAction(e -> submitDecision(false, grnBox, officerBox, parametersField, reasonField, statusLabel));

        HBox buttonRow = new HBox(10, approveButton, rejectButton);

        VBox card = new VBox(10,
                sectionTitle,
                labeled("Pending GRN", grnBox),
                labeled("QC officer", officerBox),
                labeled("Inspection notes", parametersField),
                labeled("Rejection reason", reasonField),
                buttonRow,
                statusLabel
        );
        card.getStyleClass().add("card");
        return card;
    }

    private static void submitDecision(boolean approve, ComboBox<Option> grnBox, ComboBox<Option> officerBox,
                                        TextField parametersField, TextField reasonField, Label statusLabel) {
        if (grnBox.getValue() == null) { showError(statusLabel, "Select a GRN."); return; }
        if (officerBox.getValue() == null) { showError(statusLabel, "Select the QC officer making this decision."); return; }
        if (!approve && reasonField.getText().isBlank()) { showError(statusLabel, "Enter a reason for rejecting this batch."); return; }

        try {
            JSONObject body = new JSONObject();
            body.put("qcOfficerId", officerBox.getValue().id());
            body.put("parameters", parametersField.getText());
            body.put("reason", reasonField.getText());

            String path = "/api/qc/" + grnBox.getValue().id() + (approve ? "/approve" : "/reject");
            String response = ApiClient.postForText(path, body);
            showSuccess(statusLabel, response);
        } catch (ApiClient.ApiException ex) {
            showError(statusLabel, ex.getMessage());
        }
    }

    // ---------- small shared helpers ----------

    private static VBox labeled(String labelText, Control control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        return new VBox(4, label, control);
    }

    private static Label newHiddenStatusLabel() {
        Label label = new Label();
        label.setWrapText(true);
        label.setVisible(false);
        label.setManaged(false);
        return label;
    }

    private static void showError(Label label, String message) {
        label.setText(message);
        label.getStyleClass().setAll("status-error");
        label.setVisible(true);
        label.setManaged(true);
    }

    private static void showSuccess(Label label, String message) {
        label.setText(message);
        label.getStyleClass().setAll("status-success");
        label.setVisible(true);
        label.setManaged(true);
    }

    private static Double parsePositiveDouble(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            double value = Double.parseDouble(text.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
