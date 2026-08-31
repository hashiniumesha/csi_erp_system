package com.csi.erpfrontend;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Sales & Billing module.
 *
 * DAY 2 TODO (see QCView for the pattern to follow): swap the raw numeric
 * ID TextFields (Customer, Sales Officer, Product) for ComboBoxes bound to
 * GET /api/sales/customers, GET /api/users, and
 * GET /api/inventory/finished-products, and add a Payment Type dropdown
 * validation message consistent with QCView's.
 */
public class SalesView {

    public static Node build() {
        Label title = new Label("Sales & Billing");
        title.getStyleClass().add("page-title");

        VBox layout = new VBox(20, title, buildCustomerCard(), buildInvoiceCard(), buildCollectionCard());
        layout.setPadding(new Insets(28));

        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("page-bg");
        return scrollPane;
    }

    private static VBox buildCustomerCard() {
        Label sectionTitle = new Label("Add Customer");
        sectionTitle.getStyleClass().add("section-title");

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Perera Stores - Panadura");
        TextField contactField = new TextField();
        contactField.setPromptText("e.g. 0771234567");
        TextField creditLimitField = new TextField();
        creditLimitField.setPromptText("e.g. 25000");

        Button addButton = new Button("Add Customer");
        addButton.getStyleClass().add("button-primary");
        Label statusLabel = newHiddenStatusLabel();

        addButton.setOnAction(e -> {
            if (nameField.getText().isBlank()) { showError(statusLabel, "Enter a customer name."); return; }
            Double creditLimit = parseNonNegativeDouble(creditLimitField.getText());
            if (creditLimit == null) { showError(statusLabel, "Enter a valid credit limit (0 or more)."); return; }

            try {
                JSONObject body = new JSONObject();
                body.put("name", nameField.getText().trim());
                body.put("contactNo", contactField.getText().trim());
                body.put("creditLimit", creditLimit);
                JSONObject response = ApiClient.post("/api/sales/customer", body);
                showSuccess(statusLabel, "Customer added — ID " + response.getInt("customerId") + ".");
                nameField.clear(); contactField.clear(); creditLimitField.clear();
            } catch (ApiClient.ApiException ex) {
                showError(statusLabel, ex.getMessage());
            }
        });

        VBox card = new VBox(10, sectionTitle,
                labeled("Name", nameField), labeled("Contact number", contactField), labeled("Credit limit", creditLimitField),
                addButton, statusLabel);
        card.getStyleClass().add("card");
        return card;
    }

    private static VBox buildInvoiceCard() {
        Label sectionTitle = new Label("Create Invoice (single item)");
        sectionTitle.getStyleClass().add("section-title");

        TextField customerIdField = new TextField();
        customerIdField.setPromptText("Customer ID (dropdown coming Day 2)");
        TextField officerIdField = new TextField();
        officerIdField.setPromptText("Sales officer (user) ID");
        ComboBox<String> paymentTypeBox = new ComboBox<>();
        paymentTypeBox.getItems().addAll("Cash", "Credit");
        paymentTypeBox.setPromptText("Payment type");
        paymentTypeBox.setMaxWidth(Double.MAX_VALUE);
        TextField productIdField = new TextField();
        productIdField.setPromptText("Product ID");
        TextField quantityField = new TextField();
        quantityField.setPromptText("e.g. 10");
        TextField priceField = new TextField();
        priceField.setPromptText("Unit price, e.g. 150.00");

        Button createButton = new Button("Create Invoice");
        createButton.getStyleClass().add("button-primary");
        Label statusLabel = newHiddenStatusLabel();

        createButton.setOnAction(e -> {
            Integer customerId = parsePositiveInt(customerIdField.getText());
            if (customerId == null) { showError(statusLabel, "Enter a valid Customer ID."); return; }
            Integer officerId = parsePositiveInt(officerIdField.getText());
            if (officerId == null) { showError(statusLabel, "Enter a valid Sales Officer ID."); return; }
            if (paymentTypeBox.getValue() == null) { showError(statusLabel, "Select a payment type."); return; }
            Integer productId = parsePositiveInt(productIdField.getText());
            if (productId == null) { showError(statusLabel, "Enter a valid Product ID."); return; }
            Double quantity = parsePositiveDouble(quantityField.getText());
            if (quantity == null) { showError(statusLabel, "Enter a valid quantity greater than 0."); return; }
            Double price = parsePositiveDouble(priceField.getText());
            if (price == null) { showError(statusLabel, "Enter a valid unit price greater than 0."); return; }

            try {
                JSONObject item = new JSONObject();
                item.put("productId", productId);
                item.put("quantity", quantity);
                item.put("unitPrice", price);

                JSONObject body = new JSONObject();
                body.put("customerId", customerId);
                body.put("salesOfficerId", officerId);
                body.put("paymentType", paymentTypeBox.getValue());
                body.put("items", new JSONArray().put(item));

                JSONObject response = ApiClient.post("/api/sales/invoice", body);
                showSuccess(statusLabel, "Invoice created — ID " + response.getInt("invoiceId") +
                        ", total " + response.getDouble("totalAmount") + ".");
            } catch (ApiClient.ApiException ex) {
                showError(statusLabel, ex.getMessage());
            }
        });

        VBox card = new VBox(10, sectionTitle,
                labeled("Customer ID", customerIdField), labeled("Sales officer ID", officerIdField),
                labeled("Payment type", paymentTypeBox), labeled("Product ID", productIdField),
                labeled("Quantity", quantityField), labeled("Unit price", priceField),
                createButton, statusLabel);
        card.getStyleClass().add("card");
        return card;
    }

    private static VBox buildCollectionCard() {
        Label sectionTitle = new Label("Record Credit Collection");
        sectionTitle.getStyleClass().add("section-title");

        TextField invoiceIdField = new TextField();
        invoiceIdField.setPromptText("Invoice ID");
        TextField amountField = new TextField();
        amountField.setPromptText("e.g. 500.00");

        Button recordButton = new Button("Record Collection");
        recordButton.getStyleClass().add("button-primary");
        Label statusLabel = newHiddenStatusLabel();

        recordButton.setOnAction(e -> {
            Integer invoiceId = parsePositiveInt(invoiceIdField.getText());
            if (invoiceId == null) { showError(statusLabel, "Enter a valid Invoice ID."); return; }
            Double amount = parsePositiveDouble(amountField.getText());
            if (amount == null) { showError(statusLabel, "Enter a valid amount greater than 0."); return; }

            try {
                JSONObject body = new JSONObject();
                body.put("amountCollected", amount);
                ApiClient.post("/api/sales/invoice/" + invoiceId + "/collection", body);
                showSuccess(statusLabel, "Collection recorded.");
                invoiceIdField.clear(); amountField.clear();
            } catch (ApiClient.ApiException ex) {
                showError(statusLabel, ex.getMessage());
            }
        });

        VBox card = new VBox(10, sectionTitle,
                labeled("Invoice ID", invoiceIdField), labeled("Amount collected", amountField),
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

    private static Double parseNonNegativeDouble(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            double value = Double.parseDouble(text.trim());
            return value >= 0 ? value : null;
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
