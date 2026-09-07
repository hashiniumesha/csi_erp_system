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

    public static Node build() {
        Label title = new Label("Raw Materials");
        title.getStyleClass().add("page-title");

        VBox layout = new VBox(20, title, buildAddCard(), buildEditDeleteCard(), buildMyRequestsCard());
        layout.setPadding(new Insets(28));

        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("page-bg");
        return scrollPane;
    }

    private static VBox buildAddCard() {
        Label sectionTitle = new Label("Add Raw Material");
        sectionTitle.getStyleClass().add("section-title");

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Sugar");
        TextField reorderField = new TextField();
        reorderField.setPromptText("e.g. 50");

        Button requestButton = new Button("Request");
        requestButton.getStyleClass().add("button-primary");
        Label statusLabel = newHiddenStatusLabel();

        requestButton.setOnAction(e -> {
            if (nameField.getText().isBlank()) { showError(statusLabel, "Enter a name."); return; }
            Double reorderLevel = parseNonNegativeDouble(reorderField.getText());
            if (reorderLevel == null) { showError(statusLabel, "Enter a valid reorder level (0 or more)."); return; }

            try {
                JSONObject payload = new JSONObject();
                payload.put("name", nameField.getText().trim());
                payload.put("reorderLevel", reorderLevel);
                submitRequest("CREATE", null, payload);
                showSuccess(statusLabel, "Request submitted. Please wait for a response from the Admin.");
                nameField.clear(); reorderField.clear();
            } catch (ApiClient.ApiException ex) {
                showError(statusLabel, ex.getMessage());
            }
        });

        VBox card = new VBox(10, sectionTitle,
                labeled("Name", nameField), labeled("Reorder level", reorderField),
                requestButton, statusLabel);
        card.getStyleClass().add("card");
        return card;
    }

    private static VBox buildEditDeleteCard() {
        Label sectionTitle = new Label("Edit / Delete Raw Material");
        sectionTitle.getStyleClass().add("section-title");

        ComboBox<Option> materialBox = new ComboBox<>();
        materialBox.setPromptText("Select raw material");
        materialBox.setMaxWidth(Double.MAX_VALUE);

        TextField nameField = new TextField();
        nameField.setPromptText("Name");
        TextField reorderField = new TextField();
        reorderField.setPromptText("Reorder level");
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
            if (newVal == null) { nameField.clear(); reorderField.clear(); currentStockLabel.setText(""); return; }
            JSONObject m = materialsById.get(newVal.id());
            if (m != null) {
                nameField.setText(m.getString("name"));
                reorderField.setText(String.valueOf(m.optDouble("reorderLevel", 0)));
                currentStockLabel.setText("Current stock: " + m.optDouble("currentStock", 0));
            }
        });

        Button requestChangeButton = new Button("Request Changes");
        requestChangeButton.getStyleClass().add("button-primary");
        Button requestDeleteButton = new Button("Request Delete");
        requestDeleteButton.getStyleClass().add("button-secondary");

        requestChangeButton.setOnAction(e -> {
            if (materialBox.getValue() == null) { showError(statusLabel, "Select a raw material first."); return; }
            if (nameField.getText().isBlank()) { showError(statusLabel, "Enter a name."); return; }
            Double reorderLevel = parseNonNegativeDouble(reorderField.getText());
            if (reorderLevel == null) { showError(statusLabel, "Enter a valid reorder level (0 or more)."); return; }

            try {
                JSONObject payload = new JSONObject();
                payload.put("name", nameField.getText().trim());
                payload.put("reorderLevel", reorderLevel);
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

        HBox buttonRow = new HBox(10, requestChangeButton, requestDeleteButton);

        VBox card = new VBox(10, sectionTitle,
                labeled("Raw material", materialBox), currentStockLabel,
                labeled("Name", nameField), labeled("Reorder level", reorderField),
                buttonRow, statusLabel);
        card.getStyleClass().add("card");
        return card;
    }

    // Lets the requester see Pending/Approved/Rejected without needing to
    // ask - refreshed each time this screen is opened.
    private static VBox buildMyRequestsCard() {
        Label sectionTitle = new Label("My Requests");
        sectionTitle.getStyleClass().add("section-title");

        VBox list = new VBox(8);
        try {
            JSONArray mine = ApiClient.getArray("/api/approval-requests/mine?userId=" + Session.getUserId());
            if (mine.isEmpty()) {
                Label empty = new Label("No requests yet.");
                empty.getStyleClass().add("muted-label");
                list.getChildren().add(empty);
            }
            for (int i = 0; i < mine.length(); i++) {
                list.getChildren().add(buildRequestRow(mine.getJSONObject(i)));
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

    private static HBox buildRequestRow(JSONObject r) {
        String type = r.optString("requestType", "?");
        JSONObject payload = new JSONObject(r.optString("payloadJson", "{}"));
        String description = switch (type) {
            case "CREATE" -> "Add raw material: " + payload.optString("name", "?");
            case "UPDATE" -> "Update raw material #" + r.optInt("entityId") + " → " + payload.optString("name", "?");
            case "DELETE" -> "Delete raw material #" + r.optInt("entityId");
            default -> type;
        };

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-min-width: 320;");

        String status = r.optString("status", "Pending");
        Label statusPill = new Label(status);
        statusPill.getStyleClass().add(switch (status) {
            case "Approved" -> "status-success";
            case "Rejected" -> "status-error";
            default -> "status-pending";
        });

        HBox row = new HBox(16, descLabel, statusPill);
        String note = r.optString("adminNote", "");
        if (!note.isBlank()) {
            Label noteLabel = new Label("(" + note + ")");
            noteLabel.getStyleClass().add("muted-label");
            row.getChildren().add(noteLabel);
        }
        return row;
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

    private static Double parseNonNegativeDouble(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            double value = Double.parseDouble(text.trim());
            return value >= 0 ? value : null;
        } catch (NumberFormatException e) { return null; }
    }
}
