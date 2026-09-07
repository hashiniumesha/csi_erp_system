package com.csi.erpfrontend;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

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

    private static final String[] CATEGORIES = {
            "Ice Bars", "Yogurt", "Soft Drink Cups", "Fruit Nectar", "Milk Packets", "Faluda"
    };
    private static final String[] UNIT_TYPES = { "pcs", "g", "kg", "ml", "L", "packets", "bundles" };
    private static final String[] DAMAGE_CAUSES = {
            "Pouch leak", "Dropped / mishandled", "Production defect", "Expired", "Transport damage", "Contamination"
    };

    public static Node build() {
        Label title = new Label("Inventory");
        title.getStyleClass().add("page-title");

        // 2x2 grid so the cards actually fill the screen instead of
        // stacking in one narrow column with empty space beside each one.
        // Expiry Tracking is the odd one out (5th card), so it spans the
        // full width on its own row.
        Node formGrid = FormLayout.gridOfTwo(
                buildProductCard(), buildMovementCard(), buildDamagedCard(), buildProductListCard(),
                buildExpiryTrackingCard());
        VBox layout = new VBox(20, title, formGrid);
        layout.setPadding(new Insets(28));

        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("inventory-bg");
        return scrollPane;
    }

    private static Node buildProductCard() {
        Label sectionTitle = new Label("Add Finished Product");
        sectionTitle.getStyleClass().add("section-title");

        ComboBox<String> nameBox = new ComboBox<>();
        nameBox.setEditable(true);
        nameBox.setPromptText("Select or type a name");
        try {
            for (Object o : ApiClient.getArray("/api/inventory/finished-products")) {
                nameBox.getItems().add(((JSONObject) o).getString("name"));
            }
        } catch (ApiClient.ApiException ignored) { }

        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.setEditable(true);
        categoryBox.getItems().addAll(CATEGORIES);
        categoryBox.setPromptText("Select or type a category");

        // Unit of measure is really two things together - a size (e.g. 500)
        // and a unit type (e.g. ml) - matching how products are actually
        // described ("50ml Faluda ice packet", "10L milk packets"), rather
        // than one free-text field.
        Spinner<Double> unitQuantitySpinner = new Spinner<>(0.0, 100000.0, 0.0, 1.0);
        unitQuantitySpinner.setEditable(true);

        ComboBox<String> unitTypeBox = new ComboBox<>();
        unitTypeBox.setEditable(true);
        unitTypeBox.getItems().addAll(UNIT_TYPES);
        unitTypeBox.setPromptText("Select or type a unit");

        Button addButton = new Button("Add Product");
        addButton.getStyleClass().add("button-primary");
        Label statusLabel = newHiddenStatusLabel();

        VBox inputs = new VBox(10, sectionTitle,
                labeled("Name", nameBox), labeled("Category", categoryBox),
                labeled("Unit size", unitQuantitySpinner), labeled("Unit type", unitTypeBox),
                addButton, statusLabel);
        inputs.getStyleClass().add("card");

        VBox preview = FormLayout.previewCard("Preview");
        Label nameValue = FormLayout.newPreviewValue();
        Label categoryValue = FormLayout.newPreviewValue();
        Label unitValue = FormLayout.newPreviewValue();
        preview.getChildren().addAll(
                FormLayout.previewRow("Name", nameValue),
                FormLayout.previewRow("Category", categoryValue),
                FormLayout.previewRow("Unit of measure", unitValue)
        );
        bindPreview(nameBox, nameValue);
        bindPreview(categoryBox, categoryValue);
        Runnable updateUnitPreview = () -> {
            String type = textOf(unitTypeBox);
            unitValue.setText(type.isBlank() ? "—" : unitQuantitySpinner.getValue() + " " + type);
        };
        unitQuantitySpinner.valueProperty().addListener((obs, o, n) -> updateUnitPreview.run());
        unitTypeBox.getEditor().textProperty().addListener((obs, o, n) -> updateUnitPreview.run());

        addButton.setOnAction(e -> {
            String name = textOf(nameBox), category = textOf(categoryBox), unitType = textOf(unitTypeBox);
            if (name.isBlank() || category.isBlank() || unitType.isBlank()) {
                showError(statusLabel, "Fill in the name, category, and unit of measure.");
                return;
            }
            String unitOfMeasure = unitQuantitySpinner.getValue() + " " + unitType.trim();
            try {
                JSONObject body = new JSONObject();
                body.put("name", name.trim());
                body.put("category", category.trim());
                body.put("unitOfMeasure", unitOfMeasure);
                JSONObject response = ApiClient.post("/api/inventory/finished-product", body);
                showSuccess(statusLabel, "Product added — ID " + response.getInt("productId") + ".");
                nameBox.getEditor().clear(); categoryBox.getEditor().clear(); unitTypeBox.getEditor().clear();
                nameBox.setValue(null); categoryBox.setValue(null); unitTypeBox.setValue(null);
                unitQuantitySpinner.getValueFactory().setValue(0.0);
            } catch (ApiClient.ApiException ex) {
                showError(statusLabel, ex.getMessage());
            }
        });

        return FormLayout.twoColumn(inputs, preview);
    }

    private static Node buildMovementCard() {
        Label sectionTitle = new Label("Record Finished Product Stock Movement");
        sectionTitle.getStyleClass().add("section-title");

        ComboBox<Option> productBox = new ComboBox<>();
        productBox.setPromptText("Select product");
        productBox.setMaxWidth(Double.MAX_VALUE);
        java.util.Map<Integer, String> productCategories = new java.util.HashMap<>();
        try {
            for (Object o : ApiClient.getArray("/api/inventory/finished-products")) {
                JSONObject p = (JSONObject) o;
                productBox.getItems().add(new Option(p.getInt("productId"), p.getString("name")));
                productCategories.put(p.getInt("productId"), p.optString("category", null));
            }
        } catch (ApiClient.ApiException e) {
            productBox.setPromptText("Couldn't load products");
        }

        ComboBox<String> movementTypeBox = new ComboBox<>();
        movementTypeBox.getItems().addAll("IN", "OUT");
        movementTypeBox.setPromptText("Movement type");
        movementTypeBox.setMaxWidth(Double.MAX_VALUE);

        Spinner<Double> quantitySpinner = new Spinner<>(0.0, 100000.0, 0.0, 1.0);
        quantitySpinner.setEditable(true);

        // Only meaningful for Stock In - this is what an expiry batch gets
        // stamped with, and the expiry date is then auto-calculated from
        // it (Milk Packets +2 months, Yogurt +3 months, everything else
        // +1 year) rather than typed in.
        DatePicker productionDatePicker = new DatePicker(LocalDate.now());
        Label productionDateLabel = new Label("Production / stock-in date");
        productionDateLabel.getStyleClass().add("field-label");
        VBox productionDateField = new VBox(4, productionDateLabel, productionDatePicker);
        productionDateField.setVisible(false);
        productionDateField.setManaged(false);

        Label autoExpiryHint = new Label();
        autoExpiryHint.getStyleClass().add("muted-label");
        autoExpiryHint.setWrapText(true);
        autoExpiryHint.setVisible(false);
        autoExpiryHint.setManaged(false);

        Button recordButton = new Button("Record Movement");
        recordButton.getStyleClass().add("button-primary");
        Label statusLabel = newHiddenStatusLabel();

        VBox inputs = new VBox(10, sectionTitle,
                labeled("Product", productBox), labeled("Movement type", movementTypeBox), labeled("Quantity", quantitySpinner),
                productionDateField, autoExpiryHint,
                recordButton, statusLabel);
        inputs.getStyleClass().add("card");

        VBox preview = FormLayout.previewCard("Preview");
        Label productValue = FormLayout.newPreviewValue();
        Label typeValue = FormLayout.newPreviewValue();
        Label qtyValue = FormLayout.newPreviewValue();
        Label expiryValue = FormLayout.newPreviewValue();
        preview.getChildren().addAll(
                FormLayout.previewRow("Product", productValue),
                FormLayout.previewRow("Movement type", typeValue),
                FormLayout.previewRow("Quantity", qtyValue),
                FormLayout.previewRow("Auto expiry date", expiryValue)
        );
        productBox.valueProperty().addListener((obs, o, n) -> productValue.setText(n != null ? n.toString() : "—"));
        quantitySpinner.valueProperty().addListener((obs, o, n) -> qtyValue.setText(n != null ? String.valueOf(n) : "—"));

        // Product/quantity/date all feed the same live "what expiry date
        // will this batch get" preview, computed the same way the backend
        // computes it (see ExpiryCalculator) so what's shown here matches
        // what actually gets saved.
        Runnable updateExpiryPreview = () -> {
            boolean isIn = "IN".equals(movementTypeBox.getValue());
            productionDateField.setVisible(isIn);
            productionDateField.setManaged(isIn);
            autoExpiryHint.setVisible(isIn);
            autoExpiryHint.setManaged(isIn);
            if (!isIn || productBox.getValue() == null) {
                expiryValue.setText("—");
                autoExpiryHint.setText("");
                return;
            }
            String category = productCategories.get(productBox.getValue().id());
            String name = productBox.getValue().label();
            LocalDate from = productionDatePicker.getValue() != null ? productionDatePicker.getValue() : LocalDate.now();
            LocalDate expiry = ExpiryRules.calculate(category, name, from);
            expiryValue.setText(expiry.toString());
            autoExpiryHint.setText("Expiry date is calculated automatically from the product type and this date — no need to work it out by hand.");
        };
        movementTypeBox.valueProperty().addListener((obs, o, n) -> { typeValue.setText(n != null ? n : "—"); updateExpiryPreview.run(); });
        productBox.valueProperty().addListener((obs, o, n) -> updateExpiryPreview.run());
        productionDatePicker.valueProperty().addListener((obs, o, n) -> updateExpiryPreview.run());

        recordButton.setOnAction(e -> {
            if (productBox.getValue() == null) { showError(statusLabel, "Select a product."); return; }
            if (movementTypeBox.getValue() == null) { showError(statusLabel, "Select IN or OUT."); return; }
            double quantity = quantitySpinner.getValue() != null ? quantitySpinner.getValue() : 0.0;
            if (quantity <= 0) { showError(statusLabel, "Enter a quantity greater than 0."); return; }
            boolean isIn = "IN".equals(movementTypeBox.getValue());
            if (isIn && productionDatePicker.getValue() == null) { showError(statusLabel, "Select the production / stock-in date."); return; }

            try {
                JSONObject body = new JSONObject();
                body.put("itemId", productBox.getValue().id());
                body.put("movementType", movementTypeBox.getValue());
                body.put("quantity", quantity);
                body.put("referenceType", "Manual");
                body.putOpt("referenceId", null);
                if (isIn) {
                    body.put("productionDate", productionDatePicker.getValue().toString());
                }
                ApiClient.post("/api/inventory/finished-product-movement", body);
                showSuccess(statusLabel, isIn
                        ? "Stock in recorded — batch expiry set to " + expiryValue.getText() + "."
                        : "Stock out recorded.");
                quantitySpinner.getValueFactory().setValue(0.0);
            } catch (ApiClient.ApiException ex) {
                showError(statusLabel, ex.getMessage());
            }
        });

        return FormLayout.twoColumn(inputs, preview);
    }

    private static Node buildDamagedCard() {
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

        Spinner<Double> quantitySpinner = new Spinner<>(0.0, 100000.0, 0.0, 1.0);
        quantitySpinner.setEditable(true);

        ComboBox<String> causeBox = new ComboBox<>();
        causeBox.setEditable(true);
        causeBox.getItems().addAll(DAMAGE_CAUSES);
        causeBox.setPromptText("Select or type a reason");

        ComboBox<String> stageBox = new ComboBox<>();
        stageBox.getItems().addAll("Production", "PostProduction");
        stageBox.setPromptText("Stage");
        stageBox.setMaxWidth(Double.MAX_VALUE);

        Button recordButton = new Button("Record Damaged");
        recordButton.getStyleClass().add("button-primary");
        Label statusLabel = newHiddenStatusLabel();

        VBox inputs = new VBox(10, sectionTitle,
                labeled("Product", productBox), labeled("Quantity", quantitySpinner),
                labeled("Cause", causeBox), labeled("Stage", stageBox),
                recordButton, statusLabel);
        inputs.getStyleClass().add("card");

        VBox preview = FormLayout.previewCard("Preview");
        Label productValue = FormLayout.newPreviewValue();
        Label qtyValue = FormLayout.newPreviewValue();
        Label causeValue = FormLayout.newPreviewValue();
        Label stageValue = FormLayout.newPreviewValue();
        preview.getChildren().addAll(
                FormLayout.previewRow("Product", productValue),
                FormLayout.previewRow("Quantity", qtyValue),
                FormLayout.previewRow("Cause", causeValue),
                FormLayout.previewRow("Stage", stageValue)
        );
        productBox.valueProperty().addListener((obs, o, n) -> productValue.setText(n != null ? n.toString() : "—"));
        quantitySpinner.valueProperty().addListener((obs, o, n) -> qtyValue.setText(n != null ? String.valueOf(n) : "—"));
        bindPreview(causeBox, causeValue);
        stageBox.valueProperty().addListener((obs, o, n) -> stageValue.setText(n != null ? n : "—"));

        recordButton.setOnAction(e -> {
            if (productBox.getValue() == null) { showError(statusLabel, "Select a product."); return; }
            double quantity = quantitySpinner.getValue() != null ? quantitySpinner.getValue() : 0.0;
            if (quantity <= 0) { showError(statusLabel, "Enter a quantity greater than 0."); return; }
            String cause = textOf(causeBox);
            if (cause.isBlank()) { showError(statusLabel, "Select or enter a cause."); return; }
            if (stageBox.getValue() == null) { showError(statusLabel, "Select a stage."); return; }

            try {
                JSONObject body = new JSONObject();
                body.put("productId", productBox.getValue().id());
                body.put("quantity", quantity);
                body.put("cause", cause.trim());
                body.put("stage", stageBox.getValue());
                ApiClient.post("/api/inventory/damaged", body);
                showSuccess(statusLabel, "Damaged stock recorded.");
                quantitySpinner.getValueFactory().setValue(0.0);
                causeBox.getEditor().clear(); causeBox.setValue(null);
            } catch (ApiClient.ApiException ex) {
                showError(statusLabel, ex.getMessage());
            }
        });

        return FormLayout.twoColumn(inputs, preview);
    }

    // Full-width, not the two-column input/preview shape - this card is a
    // browsable list, not a form.
    private static VBox buildProductListCard() {
        Label sectionTitle = new Label("Already Added Products");
        sectionTitle.getStyleClass().add("section-title");

        ComboBox<String> categoryFilter = new ComboBox<>();
        categoryFilter.setMaxWidth(Double.MAX_VALUE);
        VBox resultsList = new VBox(8);

        JSONArray allProducts;
        try {
            allProducts = ApiClient.getArray("/api/inventory/finished-products");
        } catch (ApiClient.ApiException e) {
            allProducts = new JSONArray();
            Label error = new Label("Couldn't load products: " + e.getMessage());
            error.getStyleClass().add("status-error");
            resultsList.getChildren().add(error);
        }
        final JSONArray products = allProducts;

        Set<String> categories = new LinkedHashSet<>();
        categories.add("All");
        for (int i = 0; i < products.length(); i++) {
            categories.add(products.getJSONObject(i).optString("category", "Uncategorized"));
        }
        categoryFilter.getItems().addAll(categories);
        categoryFilter.setValue("All");

        Runnable refresh = () -> {
            resultsList.getChildren().clear();
            String selected = categoryFilter.getValue();
            boolean any = false;
            for (int i = 0; i < products.length(); i++) {
                JSONObject p = products.getJSONObject(i);
                String category = p.optString("category", "Uncategorized");
                if (!"All".equals(selected) && !category.equals(selected)) continue;
                any = true;
                resultsList.getChildren().add(buildProductRow(p));
            }
            if (!any) {
                Label empty = new Label("No products in this category.");
                empty.getStyleClass().add("muted-label");
                resultsList.getChildren().add(empty);
            }
        };
        categoryFilter.valueProperty().addListener((obs, o, n) -> refresh.run());
        refresh.run();

        VBox card = new VBox(10, sectionTitle, labeled("Filter by category", categoryFilter), resultsList);
        card.getStyleClass().add("card");
        return card;
    }

    // Every finished-product batch, soonest-expiring first: Product Name,
    // Batch Number, Production Date, Expiry Date, Available Quantity, and
    // Expiry Status — each batch tracked separately even for the same
    // product, so two production runs of the same item can be told apart
    // by which one is closer to expiring.
    private static VBox buildExpiryTrackingCard() {
        Label sectionTitle = new Label("Expiry Tracking");
        sectionTitle.getStyleClass().add("section-title");
        Label subtitle = new Label("Every finished-product batch, soonest expiry first. Expiry dates are calculated automatically.");
        subtitle.getStyleClass().add("muted-label");

        VBox list = new VBox(8);
        try {
            JSONArray batches = ApiClient.getArray("/api/inventory/expiry-batches");
            if (batches.isEmpty()) {
                Label empty = new Label("No batches recorded yet — batches are created automatically when you record a Stock In movement above.");
                empty.getStyleClass().add("muted-label");
                empty.setWrapText(true);
                list.getChildren().add(empty);
            }
            for (int i = 0; i < batches.length(); i++) {
                list.getChildren().add(buildExpiryBatchRow(batches.getJSONObject(i)));
            }
        } catch (ApiClient.ApiException e) {
            Label error = new Label("Couldn't load expiry batches: " + e.getMessage());
            error.getStyleClass().add("status-error");
            list.getChildren().add(error);
        }

        VBox card = new VBox(10, sectionTitle, subtitle, list);
        card.getStyleClass().add("card");
        return card;
    }

    private static HBox buildExpiryBatchRow(JSONObject b) {
        Label name = new Label(b.optString("productName", "?"));
        name.setStyle("-fx-font-weight: bold; -fx-min-width: 200;");
        Label batchNo = new Label("Batch #" + b.optInt("batchId"));
        batchNo.getStyleClass().add("muted-label");
        batchNo.setStyle("-fx-min-width: 90;");
        Label production = new Label("Produced: " + b.optString("productionDate", "?"));
        production.getStyleClass().add("muted-label");
        production.setStyle("-fx-min-width: 140;");
        Label expiry = new Label("Expires: " + b.optString("expiryDate", "?"));
        expiry.setStyle("-fx-min-width: 140;");
        Label qty = new Label("Qty: " + String.format("%,.2f", b.optDouble("quantity", 0)));
        qty.setStyle("-fx-min-width: 100;");

        String status = b.optString("status", "OK");
        Label statusLabel = new Label(statusText(status, b.optLong("daysRemaining", 0)));
        statusLabel.getStyleClass().add(statusStyleClass(status));

        return new HBox(14, name, batchNo, production, expiry, qty, statusLabel);
    }

    private static String statusText(String status, long daysRemaining) {
        return switch (status) {
            case "EXPIRED" -> "Expired (" + Math.abs(daysRemaining) + "d ago)";
            case "EXPIRING_SOON" -> "Expiring soon (" + daysRemaining + "d left)";
            default -> "OK (" + daysRemaining + "d left)";
        };
    }

    private static String statusStyleClass(String status) {
        return switch (status) {
            case "EXPIRED" -> "status-error";
            case "EXPIRING_SOON" -> "status-pending";
            default -> "status-success";
        };
    }

    private static HBox buildProductRow(JSONObject p) {
        Label name = new Label(p.optString("name", "?"));
        name.setStyle("-fx-font-weight: bold; -fx-min-width: 220;");
        Label category = new Label(p.optString("category", "?"));
        category.getStyleClass().add("muted-label");
        category.setStyle("-fx-min-width: 140;");
        Label unit = new Label(p.optString("unitOfMeasure", "?"));
        unit.getStyleClass().add("muted-label");
        unit.setStyle("-fx-min-width: 100;");
        Label stock = new Label("Stock: " + p.optDouble("currentStock", 0));
        return new HBox(16, name, category, unit, stock);
    }

    // ComboBox.valueProperty() only fires when a list item is picked, not
    // while typing a custom value - listening on the editor's textProperty
    // instead keeps the preview live either way.
    private static void bindPreview(ComboBox<String> box, Label previewValue) {
        box.getEditor().textProperty().addListener((obs, o, n) -> previewValue.setText(n == null || n.isBlank() ? "—" : n));
    }

    private static String textOf(ComboBox<String> box) {
        return box.getEditor().getText() != null ? box.getEditor().getText() : "";
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
