package com.csi.erpfrontend;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Add/edit/delete here don't change data directly - Inventory Manager can
 * only submit a request (see ApprovalRequestController); the change is
 * applied only once Admin approves it, from the Messages screen.
 */
public class RawMaterialView {

    private record Option(int id, String label) {
        @Override public String toString() { return label; }
    }

    private static final String[] UNIT_TYPES = { "kg", "g", "L", "ml", "pcs", "packets", "bundles" };

    public static Node build() {
        HBox header = PageHeader.build("Raw Materials");

        // The two forms side by side, filling the width, with the request
        // history as its own full-width row below.
        Node formGrid = FormLayout.gridOfTwo(buildAddCard(), buildEditDeleteCard());
        VBox layout = new VBox(20, header, formGrid, buildMyRequestsCard());
        layout.setPadding(new Insets(28));

        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("page-bg");
        return scrollPane;
    }

    private static Node buildAddCard() {
        Label sectionTitle = new Label("Add Raw Material");
        sectionTitle.getStyleClass().add("section-title");

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Sugar");
        Spinner<Double> reorderSpinner = new Spinner<>(0.0, 100000.0, 0.0, 1.0);
        reorderSpinner.setEditable(true);
        ComboBox<String> unitBox = new ComboBox<>();
        unitBox.setEditable(true);
        unitBox.getItems().addAll(UNIT_TYPES);
        unitBox.setPromptText("Select or type a unit");

        Button requestButton = new Button("Request");
        requestButton.getStyleClass().add("button-primary");
        Label statusLabel = newHiddenStatusLabel();

        VBox inputs = new VBox(10, sectionTitle,
                labeled("Name", nameField), labeled("Reorder level", reorderSpinner), labeled("Unit type", unitBox),
                requestButton, statusLabel);
        inputs.getStyleClass().add("card");

        VBox preview = FormLayout.previewCard("Preview");
        Label nameValue = FormLayout.newPreviewValue();
        Label reorderValue = FormLayout.newPreviewValue();
        Label unitValue = FormLayout.newPreviewValue();
        preview.getChildren().addAll(
                FormLayout.previewRow("Name", nameValue),
                FormLayout.previewRow("Reorder level", reorderValue),
                FormLayout.previewRow("Unit type", unitValue)
        );
        nameField.textProperty().addListener((obs, o, n) -> nameValue.setText(n == null || n.isBlank() ? "—" : n));
        reorderSpinner.valueProperty().addListener((obs, o, n) -> reorderValue.setText(n != null ? String.valueOf(n) : "—"));
        unitBox.getEditor().textProperty().addListener((obs, o, n) -> unitValue.setText(n == null || n.isBlank() ? "—" : n));

        requestButton.setOnAction(e -> {
            if (nameField.getText().isBlank()) { showError(statusLabel, "Enter a name."); return; }
            double reorderLevel = reorderSpinner.getValue() != null ? reorderSpinner.getValue() : 0.0;
            String unit = unitBox.getEditor().getText();

            try {
                JSONObject payload = new JSONObject();
                payload.put("name", nameField.getText().trim());
                payload.put("reorderLevel", reorderLevel);
                if (unit != null && !unit.isBlank()) payload.put("unitOfMeasure", unit.trim());
                submitRequest("CREATE", null, payload);
                showSuccess(statusLabel, "Request submitted. Please wait for a response from the Admin.");
                nameField.clear();
                reorderSpinner.getValueFactory().setValue(0.0);
                unitBox.getEditor().clear(); unitBox.setValue(null);
            } catch (ApiClient.ApiException ex) {
                showError(statusLabel, ex.getMessage());
            }
        });

        return FormLayout.twoColumn(inputs, preview);
    }

    private static Node buildEditDeleteCard() {
        Label sectionTitle = new Label("Edit / Delete Raw Material");
        sectionTitle.getStyleClass().add("section-title");

        ComboBox<Option> materialBox = new ComboBox<>();
        materialBox.setPromptText("Select raw material");
        materialBox.setMaxWidth(Double.MAX_VALUE);

        TextField nameField = new TextField();
        nameField.setPromptText("Name");
        Spinner<Double> reorderSpinner = new Spinner<>(0.0, 100000.0, 0.0, 1.0);
        reorderSpinner.setEditable(true);
        ComboBox<String> unitBox = new ComboBox<>();
        unitBox.setEditable(true);
        unitBox.getItems().addAll(UNIT_TYPES);
        unitBox.setPromptText("Select or type a unit");
        Label currentStockLabel = new Label();
        currentStockLabel.getStyleClass().add("muted-label");
        Label statusLabel = newHiddenStatusLabel();

        Map<Integer, JSONObject> materialsById = new HashMap<>();
        try {
            for (Object o : ApiClient.getArray("/api/raw-materials")) {
                JSONObject m = (JSONObject) o;
                materialBox.getItems().add(new Option(m.getInt("rawMaterialId"), m.getString("name")));
                materialsById.put(m.getInt("rawMaterialId"), m);
            }
        } catch (ApiClient.ApiException e) {
            materialBox.setPromptText("Couldn't load raw materials");
        }

        materialBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                nameField.clear();
                reorderSpinner.getValueFactory().setValue(0.0);
                currentStockLabel.setText("");
                return;
            }
            JSONObject m = materialsById.get(newVal.id());
            if (m != null) {
                nameField.setText(m.getString("name"));
                reorderSpinner.getValueFactory().setValue(m.optDouble("reorderLevel", 0));
                currentStockLabel.setText("Current stock: " + m.optDouble("currentStock", 0));
                unitBox.setValue(m.optString("unitOfMeasure", null));
            }
        });

        Button requestChangeButton = new Button("Request Changes");
        requestChangeButton.getStyleClass().add("button-primary");
        Button requestDeleteButton = new Button("Request Delete");
        requestDeleteButton.getStyleClass().add("button-danger");

        VBox inputs = new VBox(10, sectionTitle,
                labeled("Raw material", materialBox), currentStockLabel,
                labeled("Name", nameField), labeled("Reorder level", reorderSpinner), labeled("Unit type", unitBox),
                new HBox(10, requestChangeButton, requestDeleteButton), statusLabel);
        inputs.getStyleClass().add("card");

        VBox preview = FormLayout.previewCard("Preview");
        Label materialValue = FormLayout.newPreviewValue();
        Label nameValue = FormLayout.newPreviewValue();
        Label reorderValue = FormLayout.newPreviewValue();
        Label unitValue = FormLayout.newPreviewValue();
        Label stockValue = FormLayout.newPreviewValue();
        preview.getChildren().addAll(
                FormLayout.previewRow("Raw material", materialValue),
                FormLayout.previewRow("New name", nameValue),
                FormLayout.previewRow("New reorder level", reorderValue),
                FormLayout.previewRow("New unit type", unitValue),
                FormLayout.previewRow("Current stock", stockValue)
        );
        materialBox.valueProperty().addListener((obs, o, n) -> materialValue.setText(n != null ? n.toString() : "—"));
        nameField.textProperty().addListener((obs, o, n) -> nameValue.setText(n == null || n.isBlank() ? "—" : n));
        reorderSpinner.valueProperty().addListener((obs, o, n) -> reorderValue.setText(n != null ? String.valueOf(n) : "—"));
        unitBox.getEditor().textProperty().addListener((obs, o, n) -> unitValue.setText(n == null || n.isBlank() ? "—" : n));
        currentStockLabel.textProperty().addListener((obs, o, n) -> stockValue.setText(n == null || n.isBlank() ? "—" : n));

        requestChangeButton.setOnAction(e -> {
            if (materialBox.getValue() == null) { showError(statusLabel, "Select a raw material first."); return; }
            if (nameField.getText().isBlank()) { showError(statusLabel, "Enter a name."); return; }
            double reorderLevel = reorderSpinner.getValue() != null ? reorderSpinner.getValue() : 0.0;
            String unit = unitBox.getEditor().getText();

            try {
                JSONObject payload = new JSONObject();
                payload.put("name", nameField.getText().trim());
                payload.put("reorderLevel", reorderLevel);
                payload.put("unitOfMeasure", unit != null ? unit.trim() : "");
                submitRequest("UPDATE", materialBox.getValue().id(), payload);
                showSuccess(statusLabel, "Request submitted. Please wait for a response from the Admin.");
            } catch (ApiClient.ApiException ex) {
                showError(statusLabel, ex.getMessage());
            }
        });

        requestDeleteButton.setOnAction(e -> {
            if (materialBox.getValue() == null) { showError(statusLabel, "Select a raw material first."); return; }
            try {
                submitRequest("DELETE", materialBox.getValue().id(), new JSONObject());
                showSuccess(statusLabel, "Delete request submitted. Please wait for a response from the Admin.");
            } catch (ApiClient.ApiException ex) {
                showError(statusLabel, ex.getMessage());
            }
        });

        return FormLayout.twoColumn(inputs, preview);
    }

    // Lets the requester see Pending/Approved/Rejected without needing to
    // ask - refreshed each time this screen is opened.
    private static VBox buildMyRequestsCard() {
        Label sectionTitle = new Label("My Requests");
        sectionTitle.getStyleClass().add("section-title");

        VBox list = new VBox(12);
        try {
            JSONArray mine = ApiClient.getArray("/api/approval-requests/mine?userId=" + Session.getUserId());
            if (mine.isEmpty()) {
                Label empty = new Label("No requests yet.");
                empty.getStyleClass().add("muted-label");
                list.getChildren().add(empty);
            }
            for (int i = 0; i < mine.length(); i++) {
                list.getChildren().add(NotificationCard.build(mine.getJSONObject(i)));
            }
        } catch (ApiClient.ApiException e) {
            Label error = new Label("Couldn't load your requests: " + e.getMessage());
            error.getStyleClass().add("status-error");
            list.getChildren().add(error);
        }

        VBox card = new VBox(10, sectionTitle, list);
        card.getStyleClass().add("card");
        return card;
    }

    private static void submitRequest(String requestType, Integer entityId, JSONObject payload) {
        JSONObject body = new JSONObject();
        body.put("requestType", requestType);
        body.put("entityType", "RawMaterial");
        body.putOpt("entityId", entityId);
        body.put("payload", payload);
        body.put("requestedByUserId", Session.getUserId());
        ApiClient.post("/api/approval-requests", body);
    }

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
}
