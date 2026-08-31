package com.csi.erpfrontend;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.json.JSONObject;

/**
 * Inventory module.
 */
public class InventoryView {

    // Pairs a database ID with the text shown in the dropdown. Typing
    // ".toString()" on one of these returns the label, so the ComboBox
    // shows the name instead of a raw ID.
    private record Option(int id, String label) {
        @Override public String toString() { return label; }
    }

    public static Node build() {
        Label title = new Label("Inventory");
        title.getStyleClass().add("page-title");

        VBox layout = new VBox(20, title, buildProductCard(), buildMovementCard(), buildDamagedCard());
        layout.setPadding(new Insets(28));

        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("page-bg");
        return scrollPane;
    }

    private static VBox buildProductCard() {
        Label sectionTitle = new Label("Add Finished Product");
        sectionTitle.getStyleClass().add("section-title");

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Ice Bar - Mixed Fruit");
        TextField categoryField = new TextField();
        categoryField.setPromptText("e.g. Ice Bars");
        TextField unitField = new TextField();
        unitField.setPromptText("e.g. pcs, kg");

        Button addButton = new Button("Add Product");
        addButton.getStyleClass().add("button-primary");
        Label statusLabel = newHiddenStatusLabel();

        addButton.setOnAction(e -> {
            if (nameField.getText().isBlank() || categoryField.getText().isBlank() || unitField.getText().isBlank()) {
                showError(statusLabel, "Fill in the name, category, and unit of measure.");
                return;
            }
            try {
                JSONObject body = new JSONObject();
                body.put("name", nameField.getText().trim());
                body.put("category", categoryField.getText().trim());
                body.put("unitOfMeasure", unitField.getText().trim());
                JSONObject response = ApiClient.post("/api/inventory/finished-product", body);
                showSuccess(statusLabel, "Product added — ID " + response.getInt("productId") + ".");
                nameField.clear(); categoryField.clear(); unitField.clear();
            } catch (ApiClient.ApiException ex) {
                showError(statusLabel, ex.getMessage());
            }
        });

        VBox card = new VBox(10, sectionTitle,
                labeled("Name", nameField), labeled("Category", categoryField), labeled("Unit of measure", unitField),
                addButton, statusLabel);
        card.getStyleClass().add("card");
        return card;
    }

    private static VBox buildMovementCard() {
        Label sectionTitle = new Label("Record Finished Product Stock Movement");
        sectionTitle.getStyleClass().add("section-title");

        ComboBox<Option> productBox = new ComboBox<>();
        productBox.setPromptText("Select product");
        productBox.setMaxWidth(Double.MAX_VALUE);
        try {
            for (Object o : ApiClient.getArray("/api/inventory/finished-products")) {
                JSONObject p = (JSONObject) o;
                productBox.getItems().add(new Option(p.getInt("productId"), p.getString("name")));
            }
        } catch (ApiClient.ApiException e) {
            productBox.setPromptText("Couldn't load products");
        }

        ComboBox<String> movementTypeBox = new ComboBox<>();
        movementTypeBox.getItems().addAll("IN", "OUT");
        movementTypeBox.setPromptText("Movement type");
        movementTypeBox.setMaxWidth(Double.MAX_VALUE);
        TextField quantityField = new TextField();
        quantityField.setPromptText("e.g. 25");

        Button recordButton = new Button("Record Movement");
        recordButton.getStyleClass().add("button-primary");
        Label statusLabel = newHiddenStatusLabel();

        recordButton.setOnAction(e -> {
            if (productBox.getValue() == null) { showError(statusLabel, "Select a product."); return; }
            if (movementTypeBox.getValue() == null) { showError(statusLabel, "Select IN or OUT."); return; }
            Double quantity = parsePositiveDouble(quantityField.getText());
            if (quantity == null) { showError(statusLabel, "Enter a valid quantity greater than 0."); return; }

            try {
                JSONObject body = new JSONObject();
                body.put("itemId", productBox.getValue().id());
                body.put("movementType", movementTypeBox.getValue());
                body.put("quantity", quantity);
                body.put("referenceType", "Manual");
                body.putOpt("referenceId", null);
                ApiClient.post("/api/inventory/finished-product-movement", body);
                showSuccess(statusLabel, "Movement recorded.");
                quantityField.clear();
            } catch (ApiClient.ApiException ex) {
                showError(statusLabel, ex.getMessage());
            }
        });

        VBox card = new VBox(10, sectionTitle,
                labeled("Product", productBox), labeled("Movement type", movementTypeBox), labeled("Quantity", quantityField),
                recordButton, statusLabel);
        card.getStyleClass().add("card");
        return card;
    }

    private static VBox buildDamagedCard() {
        Label sectionTitle = new Label("Record Damaged Product");
        sectionTitle.getStyleClass().add("section-title");

        ComboBox<Option> productBox = new ComboBox<>();
        productBox.setPromptText("Select product");
        productBox.setMaxWidth(Double.MAX_VALUE);
        try {
            for (Object o : ApiClient.getArray("/api/inventory/finished-products")) {
                JSONObject p = (JSONObject) o;
                productBox.getItems().add(new Option(p.getInt("productId"), p.getString("name")));
            }
        } catch (ApiClient.ApiException e) {
            productBox.setPromptText("Couldn't load products");
        }

        TextField quantityField = new TextField();
        quantityField.setPromptText("e.g. 5");
        TextField causeField = new TextField();
        causeField.setPromptText("e.g. Pouch leak");
        ComboBox<String> stageBox = new ComboBox<>();
        stageBox.getItems().addAll("Production", "PostProduction");
        stageBox.setPromptText("Stage");
        stageBox.setMaxWidth(Double.MAX_VALUE);

        Button recordButton = new Button("Record Damaged");
        recordButton.getStyleClass().add("button-primary");
        Label statusLabel = newHiddenStatusLabel();

        recordButton.setOnAction(e -> {
            if (productBox.getValue() == null) { showError(statusLabel, "Select a product."); return; }
            Double quantity = parsePositiveDouble(quantityField.getText());
            if (quantity == null) { showError(statusLabel, "Enter a valid quantity greater than 0."); return; }
            if (causeField.getText().isBlank()) { showError(statusLabel, "Enter a cause."); return; }
            if (stageBox.getValue() == null) { showError(statusLabel, "Select a stage."); return; }

            try {
                JSONObject body = new JSONObject();
                body.put("productId", productBox.getValue().id());
                body.put("quantity", quantity);
                body.put("cause", causeField.getText().trim());
                body.put("stage", stageBox.getValue());
                ApiClient.post("/api/inventory/damaged", body);
                showSuccess(statusLabel, "Damaged stock recorded.");
                quantityField.clear(); causeField.clear();
            } catch (ApiClient.ApiException ex) {
                showError(statusLabel, ex.getMessage());
            }
        });

        VBox card = new VBox(10, sectionTitle,
                labeled("Product", productBox), labeled("Quantity", quantityField),
                labeled("Cause", causeField), labeled("Stage", stageBox),
                recordButton, statusLabel);
        card.getStyleClass().add("card");
        return card;
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

    private static Double parsePositiveDouble(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            double value = Double.parseDouble(text.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException e) { return null; }
    }

    private static Integer parsePositiveInt(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            int value = Integer.parseInt(text.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException e) { return null; }
    }
}
