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
 * Sales & Billing module.
 */
public class SalesView {

    // Pairs a database ID with the text shown in the dropdown. Typing
    // ".toString()" on one of these returns the label, so the ComboBox
    // shows the name instead of a raw ID.
    private record Option(int id, String label) {
        @Override public String toString() { return label; }
    }

    public static Node build() {
        HBox header = PageHeader.build("Sales & Billing");

        VBox invoiceList = new VBox(8);
        refreshInvoiceList(invoiceList);

        VBox layout = new VBox(20, header, buildCustomerCard(), buildInvoiceCard(invoiceList), buildCollectionCard(),
                buildInvoiceListCard(invoiceList));
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

        private static VBox buildInvoiceCard(VBox invoiceList) {
        Label sectionTitle = new Label("Create Invoice (single item)");
        sectionTitle.getStyleClass().add("section-title");

        ComboBox<Option> customerBox = new ComboBox<>();
        customerBox.setPromptText("Select customer");
        customerBox.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Option> officerBox = new ComboBox<>();
        officerBox.setPromptText("Select sales officer");
        officerBox.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Option> productBox = new ComboBox<>();
        productBox.setPromptText("Select product");
        productBox.setMaxWidth(Double.MAX_VALUE);

        Map<Integer, String> productUnits = new HashMap<>();
        try {
            for (Object o : ApiClient.getArray("/api/sales/customers")) {
                JSONObject c = (JSONObject) o;
                customerBox.getItems().add(new Option(c.getInt("customerId"), c.getString("name")));
            }
            for (Object o : ApiClient.getArray("/api/users")) {
                JSONObject u = (JSONObject) o;
                String role = u.optString("roleName", "");
                if (!"Sales Officer".equals(role) && !"Admin".equals(role)) continue;
                officerBox.getItems().add(new Option(u.getInt("userId"), u.getString("fullName") + " (" + role + ")"));
            }
            for (Object o : ApiClient.getArray("/api/inventory/finished-products")) {
                JSONObject p = (JSONObject) o;
                productBox.getItems().add(new Option(p.getInt("productId"), p.getString("name")));
                productUnits.put(p.getInt("productId"), p.optString("unitOfMeasure", null));
            }
        } catch (ApiClient.ApiException e) {
            customerBox.setPromptText("Couldn't load customers");
            officerBox.setPromptText("Couldn't load sales officers");
            productBox.setPromptText("Couldn't load products");
        }

        ComboBox<String> paymentTypeBox = new ComboBox<>();
        paymentTypeBox.getItems().addAll("Cash", "Credit");
        paymentTypeBox.setPromptText("Payment type");
        paymentTypeBox.setMaxWidth(Double.MAX_VALUE);
        TextField quantityField = new TextField();
        quantityField.setPromptText("e.g. 10");

        // Read-only, sourced from the product's own stored unit (set when
        // it was added in Inventory) rather than a second, disconnected
        // dropdown that wouldn't actually be saved anywhere on the invoice.
        Label quantityUnitHint = new Label();
        quantityUnitHint.getStyleClass().add("muted-label");
        productBox.valueProperty().addListener((obs, o, n) -> {
            String unit = n != null ? productUnits.get(n.id()) : null;
            quantityUnitHint.setText(unit != null && !unit.isBlank() ? "Unit: " + unit : "Unit not set for this product");
        });

        TextField priceField = new TextField();
        priceField.setPromptText("Unit price, e.g. 150.00");

        Button createButton = new Button("Create Invoice");
        createButton.getStyleClass().add("button-primary");
        Label statusLabel = newHiddenStatusLabel();

        createButton.setOnAction(e -> {
            if (customerBox.getValue() == null) { showError(statusLabel, "Select a customer."); return; }
            if (officerBox.getValue() == null) { showError(statusLabel, "Select a sales officer."); return; }
            if (paymentTypeBox.getValue() == null) { showError(statusLabel, "Select a payment type."); return; }
            if (productBox.getValue() == null) { showError(statusLabel, "Select a product."); return; }
            Double quantity = parsePositiveDouble(quantityField.getText());
            if (quantity == null) { showError(statusLabel, "Enter a valid quantity greater than 0."); return; }
            Double price = parsePositiveDouble(priceField.getText());
            if (price == null) { showError(statusLabel, "Enter a valid unit price greater than 0."); return; }

            try {
                JSONObject item = new JSONObject();
                item.put("productId", productBox.getValue().id());
                item.put("quantity", quantity);
                item.put("unitPrice", price);

                JSONObject body = new JSONObject();
                body.put("customerId", customerBox.getValue().id());
                body.put("salesOfficerId", officerBox.getValue().id());
                body.put("paymentType", paymentTypeBox.getValue());
                body.put("items", new JSONArray().put(item));

                JSONObject response = ApiClient.post("/api/sales/invoice", body);
                int newInvoiceId = response.getInt("invoiceId");
                showSuccess(statusLabel, "Invoice #" + newInvoiceId + " created — opening preview…");
                refreshInvoiceList(invoiceList);
                // A message alone doesn't show what was actually billed -
                // open the real invoice document straight away.
                InvoicePreviewView.show(newInvoiceId);
            } catch (ApiClient.ApiException ex) {
                showError(statusLabel, ex.getMessage());
            }
        });

        VBox card = new VBox(10, sectionTitle,
                labeled("Customer", customerBox), labeled("Sales officer", officerBox),
                labeled("Payment type", paymentTypeBox), labeled("Product", productBox),
                labeled("Quantity", quantityField), quantityUnitHint, labeled("Unit price", priceField),
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
                body.put("invoiceId", invoiceId);
                body.put("amount", amount);
                ApiClient.post("/api/sales/collection", body);
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

    // Answers "where did the invoice go" - every invoice created (by
    // anyone) shows up here, most recent last, so a Sales Officer can
    // confirm their own invoices actually saved without needing a
    // separate download/export.
    private static VBox buildInvoiceListCard(VBox list) {
        Label sectionTitle = new Label("Invoice List");
        sectionTitle.getStyleClass().add("section-title");

        VBox card = new VBox(10, sectionTitle, list);
        card.getStyleClass().add("card");
        return card;
    }

    private static void refreshInvoiceList(VBox list) {
        list.getChildren().clear();
        try {
            JSONArray invoices = ApiClient.getArray("/api/sales/invoices");
            if (invoices.isEmpty()) {
                Label empty = new Label("No invoices yet.");
                empty.getStyleClass().add("muted-label");
                list.getChildren().add(empty);
            }
            for (int i = 0; i < invoices.length(); i++) {
                list.getChildren().add(buildInvoiceRow(invoices.getJSONObject(i)));
            }
        } catch (ApiClient.ApiException e) {
            Label error = new Label("Couldn't load invoices: " + e.getMessage());
            error.getStyleClass().add("status-error");
            list.getChildren().add(error);
        }
    }

    private static HBox buildInvoiceRow(JSONObject inv) {
        int invoiceId = inv.optInt("invoiceId");
        Label id = new Label("Invoice #" + invoiceId);
        id.setStyle("-fx-font-weight: bold; -fx-min-width: 100;");
        Label customer = new Label(inv.optString("customerName", "?"));
        customer.setStyle("-fx-min-width: 220;");
        Label officer = new Label(inv.optString("salesOfficerName", "?"));
        officer.getStyleClass().add("muted-label");
        officer.setStyle("-fx-min-width: 160;");
        Label type = new Label(inv.optString("paymentType", "?"));
        type.getStyleClass().add("muted-label");
        type.setStyle("-fx-min-width: 70;");
        Label total = new Label("Rs. " + String.format("%,.2f", inv.optDouble("totalAmount", 0)));
        total.setStyle("-fx-min-width: 120;");
        Label date = new Label(inv.optString("invoiceDate", "?"));
        date.getStyleClass().add("muted-label");
        date.setStyle("-fx-min-width: 90;");

        // Opens the actual invoice document (logo, itemized products,
        // totals, Print button) instead of leaving "created successfully"
        // as the only trace an invoice ever existed.
        Button view = new Button("View");
        view.getStyleClass().add("button-secondary");
        view.setOnAction(e -> InvoicePreviewView.show(invoiceId));

        HBox row = new HBox(16, id, customer, officer, type, total, date, view);
        row.getStyleClass().add("data-row");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
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
