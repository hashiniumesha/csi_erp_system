package com.csi.erpfrontend;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;

public class MainApp extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Ceylon Sweets Island ERP - Login");
        primaryStage.setScene(buildLoginScene());
        primaryStage.show();
    }

    private Scene buildLoginScene() {
        Label titleLabel = new Label("Ceylon Sweets Island ERP");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label usernameLabel = new Label("Username:");
        TextField usernameField = new TextField();

        Label passwordLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();

        Button loginButton = new Button("Login");
        Label statusLabel = new Label();

        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            try {
                String jsonBody = String.format(
                    "{\"username\":\"%s\",\"password\":\"%s\"}", username, password);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JSONObject json = new JSONObject(response.body());
                    String fullName = json.getString("fullName");
                    statusLabel.setStyle("-fx-text-fill: green;");
                    statusLabel.setText("Login successful! Welcome " + fullName);

                    primaryStage.setTitle("Ceylon Sweets Island ERP - Dashboard");
                    primaryStage.setScene(buildDashboardScene());
                } else {
                    statusLabel.setStyle("-fx-text-fill: red;");
                    statusLabel.setText("Login failed: " + response.body());
                }
            } catch (Exception ex) {
                statusLabel.setStyle("-fx-text-fill: red;");
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.getChildren().addAll(titleLabel, usernameLabel, usernameField, passwordLabel, passwordField, loginButton, statusLabel);

        return new Scene(layout, 400, 300);
    }

    private Scene buildDashboardScene() {
        Label titleLabel = new Label("Ceylon Sweets Island ERP");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button qcButton = new Button("Production / QC");
        Button inventoryButton = new Button("Inventory");
        Button salesButton = new Button("Sales & Billing");

        qcButton.setOnAction(e -> primaryStage.setScene(buildQCScene()));
        inventoryButton.setOnAction(e -> primaryStage.setScene(buildInventoryScene()));
        salesButton.setOnAction(e -> primaryStage.setScene(buildSalesScene()));

        VBox layout = new VBox(12);
        layout.setPadding(new Insets(24));
        layout.getChildren().addAll(titleLabel, qcButton, inventoryButton, salesButton);

        return new Scene(layout, 340, 240);
    }

    private Scene buildQCScene() {
        Label titleLabel = new Label("Production / QC Module");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button backButton = new Button("← Back to Dashboard");
        backButton.setOnAction(e -> primaryStage.setScene(buildDashboardScene()));

        Label grnSectionLabel = new Label("Submit GRN (Goods Received Note)");
        grnSectionLabel.setStyle("-fx-font-weight: bold;");

        TextField supplierIdField = new TextField();
        supplierIdField.setPromptText("Supplier ID");

        TextField rawMaterialIdField = new TextField();
        rawMaterialIdField.setPromptText("Raw Material ID");

        TextField quantityField = new TextField();
        quantityField.setPromptText("Quantity Received");

        TextField unitCostField = new TextField();
        unitCostField.setPromptText("Unit Cost");

        Button submitGrnButton = new Button("Submit GRN");
        Label grnStatusLabel = new Label();

        submitGrnButton.setOnAction(e -> {
            try {
                String jsonBody = String.format(
                    "{\"supplierId\":%s,\"rawMaterialId\":%s,\"quantityReceived\":%s,\"unitCost\":%s}",
                    supplierIdField.getText(), rawMaterialIdField.getText(),
                    quantityField.getText(), unitCostField.getText());

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/grn"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JSONObject json = new JSONObject(response.body());
                    grnStatusLabel.setStyle("-fx-text-fill: green;");
                    grnStatusLabel.setText("GRN submitted! ID: " + json.getInt("grnId") + ", Status: " + json.getString("status"));
                } else {
                    grnStatusLabel.setStyle("-fx-text-fill: red;");
                    grnStatusLabel.setText("Failed: " + response.body());
                }
            } catch (Exception ex) {
                grnStatusLabel.setStyle("-fx-text-fill: red;");
                grnStatusLabel.setText("Error: " + ex.getMessage());
            }
        });

        Label decisionSectionLabel = new Label("QC Decision (Approve / Reject)");
        decisionSectionLabel.setStyle("-fx-font-weight: bold;");

        TextField grnIdField = new TextField();
        grnIdField.setPromptText("GRN ID");

        TextField qcOfficerIdField = new TextField();
        qcOfficerIdField.setPromptText("QC Officer ID");

        TextField parametersField = new TextField();
        parametersField.setPromptText("Parameters / Notes");

        TextField reasonField = new TextField();
        reasonField.setPromptText("Reason (required for reject)");

        Button approveButton = new Button("Approve");
        Button rejectButton = new Button("Reject");
        Label decisionStatusLabel = new Label();

        approveButton.setOnAction(e -> sendDecision(true, grnIdField.getText(), qcOfficerIdField.getText(),
                parametersField.getText(), reasonField.getText(), decisionStatusLabel));

        rejectButton.setOnAction(e -> sendDecision(false, grnIdField.getText(), qcOfficerIdField.getText(),
                parametersField.getText(), reasonField.getText(), decisionStatusLabel));

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.getChildren().addAll(
                backButton, titleLabel,
                grnSectionLabel, supplierIdField, rawMaterialIdField, quantityField, unitCostField, submitGrnButton, grnStatusLabel,
                new Separator(),
                decisionSectionLabel, grnIdField, qcOfficerIdField, parametersField, reasonField, approveButton, rejectButton, decisionStatusLabel
        );

        return new Scene(layout, 460, 640);
    }

    private void sendDecision(boolean approve, String grnId, String qcOfficerId, String parameters, String reason, Label statusLabel) {
        try {
            String jsonBody = String.format(
                "{\"qcOfficerId\":%s,\"parameters\":\"%s\",\"reason\":\"%s\"}",
                qcOfficerId, parameters, reason);

            String path = approve ? "/approve" : "/reject";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/qc/" + grnId + path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                statusLabel.setStyle("-fx-text-fill: green;");
                statusLabel.setText(response.body());
            } else {
                statusLabel.setStyle("-fx-text-fill: red;");
                statusLabel.setText("Failed: " + response.body());
            }
        } catch (Exception ex) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }

    private Scene buildInventoryScene() {
        Label titleLabel = new Label("Inventory Module");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button backButton = new Button("← Back to Dashboard");
        backButton.setOnAction(e -> primaryStage.setScene(buildDashboardScene()));

        Label productSectionLabel = new Label("Add Finished Product");
        productSectionLabel.setStyle("-fx-font-weight: bold;");
        TextField productNameField = new TextField(); productNameField.setPromptText("Name");
        TextField categoryField = new TextField(); categoryField.setPromptText("Category");
        TextField unitField = new TextField(); unitField.setPromptText("Unit of Measure (e.g. pcs, kg)");
        Button addProductButton = new Button("Add Product");
        Label productStatusLabel = new Label();

        addProductButton.setOnAction(e -> {
            try {
                String jsonBody = String.format(
                    "{\"name\":\"%s\",\"category\":\"%s\",\"unitOfMeasure\":\"%s\"}",
                    productNameField.getText(), categoryField.getText(), unitField.getText());
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/inventory/finished-product"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JSONObject json = new JSONObject(response.body());
                    productStatusLabel.setStyle("-fx-text-fill: green;");
                    productStatusLabel.setText("Product added! ID: " + json.getInt("productId"));
                } else {
                    productStatusLabel.setStyle("-fx-text-fill: red;");
                    productStatusLabel.setText("Failed: " + response.body());
                }
            } catch (Exception ex) {
                productStatusLabel.setStyle("-fx-text-fill: red;");
                productStatusLabel.setText("Error: " + ex.getMessage());
            }
        });

        Label movementSectionLabel = new Label("Record Finished Product Stock Movement");
        movementSectionLabel.setStyle("-fx-font-weight: bold;");
        TextField movementProductIdField = new TextField(); movementProductIdField.setPromptText("Product ID");
        ComboBox<String> movementTypeBox = new ComboBox<>();
        movementTypeBox.getItems().addAll("IN", "OUT");
        movementTypeBox.setPromptText("Movement Type");
        TextField movementQtyField = new TextField(); movementQtyField.setPromptText("Quantity");
        Button recordMovementButton = new Button("Record Movement");
        Label movementStatusLabel = new Label();

        recordMovementButton.setOnAction(e -> {
            try {
                String jsonBody = String.format(
                    "{\"itemId\":%s,\"movementType\":\"%s\",\"quantity\":%s,\"referenceType\":\"Manual\",\"referenceId\":null}",
                    movementProductIdField.getText(), movementTypeBox.getValue(), movementQtyField.getText());
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/inventory/finished-product-movement"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    movementStatusLabel.setStyle("-fx-text-fill: green;");
                    movementStatusLabel.setText("Movement recorded.");
                } else {
                    movementStatusLabel.setStyle("-fx-text-fill: red;");
                    movementStatusLabel.setText("Failed: " + response.body());
                }
            } catch (Exception ex) {
                movementStatusLabel.setStyle("-fx-text-fill: red;");
                movementStatusLabel.setText("Error: " + ex.getMessage());
            }
        });

        Label damagedSectionLabel = new Label("Record Damaged Product");
        damagedSectionLabel.setStyle("-fx-font-weight: bold;");
        TextField damagedProductIdField = new TextField(); damagedProductIdField.setPromptText("Product ID");
        TextField damagedQtyField = new TextField(); damagedQtyField.setPromptText("Quantity");
        TextField causeField = new TextField(); causeField.setPromptText("Cause");
        ComboBox<String> stageBox = new ComboBox<>();
        stageBox.getItems().addAll("Production", "PostProduction");
        stageBox.setPromptText("Stage");
        Button recordDamagedButton = new Button("Record Damaged");
        Label damagedStatusLabel = new Label();

        recordDamagedButton.setOnAction(e -> {
            try {
                String jsonBody = String.format(
                    "{\"productId\":%s,\"quantity\":%s,\"cause\":\"%s\",\"stage\":\"%s\"}",
                    damagedProductIdField.getText(), damagedQtyField.getText(), causeField.getText(), stageBox.getValue());
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/inventory/damaged"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    damagedStatusLabel.setStyle("-fx-text-fill: green;");
                    damagedStatusLabel.setText("Damaged stock recorded.");
                } else {
                    damagedStatusLabel.setStyle("-fx-text-fill: red;");
                    damagedStatusLabel.setText("Failed: " + response.body());
                }
            } catch (Exception ex) {
                damagedStatusLabel.setStyle("-fx-text-fill: red;");
                damagedStatusLabel.setText("Error: " + ex.getMessage());
            }
        });

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.getChildren().addAll(
                backButton, titleLabel,
                productSectionLabel, productNameField, categoryField, unitField, addProductButton, productStatusLabel,
                new Separator(),
                movementSectionLabel, movementProductIdField, movementTypeBox, movementQtyField, recordMovementButton, movementStatusLabel,
                new Separator(),
                damagedSectionLabel, damagedProductIdField, damagedQtyField, causeField, stageBox, recordDamagedButton, damagedStatusLabel
        );

        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);
        return new Scene(scrollPane, 480, 640);
    }

    private Scene buildSalesScene() {
        Label titleLabel = new Label("Sales & Billing Module");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button backButton = new Button("← Back to Dashboard");
        backButton.setOnAction(e -> primaryStage.setScene(buildDashboardScene()));

        Label customerSectionLabel = new Label("Add Customer");
        customerSectionLabel.setStyle("-fx-font-weight: bold;");
        TextField customerNameField = new TextField(); customerNameField.setPromptText("Name");
        TextField contactField = new TextField(); contactField.setPromptText("Contact No");
        TextField creditLimitField = new TextField(); creditLimitField.setPromptText("Credit Limit");
        Button addCustomerButton = new Button("Add Customer");
        Label customerStatusLabel = new Label();

        addCustomerButton.setOnAction(e -> {
            try {
                String jsonBody = String.format(
                    "{\"name\":\"%s\",\"contactNo\":\"%s\",\"creditLimit\":%s}",
                    customerNameField.getText(), contactField.getText(), creditLimitField.getText());
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/sales/customer"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JSONObject json = new JSONObject(response.body());
                    customerStatusLabel.setStyle("-fx-text-fill: green;");
                    customerStatusLabel.setText("Customer added! ID: " + json.getInt("customerId"));
                } else {
                    customerStatusLabel.setStyle("-fx-text-fill: red;");
                    customerStatusLabel.setText("Failed: " + response.body());
                }
            } catch (Exception ex) {
                customerStatusLabel.setStyle("-fx-text-fill: red;");
                customerStatusLabel.setText("Error: " + ex.getMessage());
            }
        });

        Label invoiceSectionLabel = new Label("Create Invoice (single item)");
        invoiceSectionLabel.setStyle("-fx-font-weight: bold;");
        TextField invCustomerIdField = new TextField(); invCustomerIdField.setPromptText("Customer ID");
        TextField invOfficerIdField = new TextField(); invOfficerIdField.setPromptText("Sales Officer (User) ID");
        ComboBox<String> paymentTypeBox = new ComboBox<>();
        paymentTypeBox.getItems().addAll("Cash", "Credit");
        paymentTypeBox.setPromptText("Payment Type");
        TextField invProductIdField = new TextField(); invProductIdField.setPromptText("Product ID");
        TextField invQtyField = new TextField(); invQtyField.setPromptText("Quantity");
        TextField invPriceField = new TextField(); invPriceField.setPromptText("Unit Price");
        Button createInvoiceButton = new Button("Create Invoice");
        Label invoiceStatusLabel = new Label();

        createInvoiceButton.setOnAction(e -> {
            try {
                String jsonBody = String.format(
                    "{\"customerId\":%s,\"salesOfficerId\":%s,\"paymentType\":\"%s\",\"items\":[{\"productId\":%s,\"quantity\":%s,\"unitPrice\":%s}]}",
                    invCustomerIdField.getText(), invOfficerIdField.getText(), paymentTypeBox.getValue(),
                    invProductIdField.getText(), invQtyField.getText(), invPriceField.getText());
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/sales/invoice"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JSONObject json = new JSONObject(response.body());
                    invoiceStatusLabel.setStyle("-fx-text-fill: green;");
                    invoiceStatusLabel.setText("Invoice created! ID: " + json.getInt("invoiceId") + ", Total: " + json.getDouble("totalAmount"));
                } else {
                    invoiceStatusLabel.setStyle("-fx-text-fill: red;");
                    invoiceStatusLabel.setText("Failed: " + response.body());
                }
            } catch (Exception ex) {
                invoiceStatusLabel.setStyle("-fx-text-fill: red;");
                invoiceStatusLabel.setText("Error: " + ex.getMessage());
            }
        });

        Label collectionSectionLabel = new Label("Record Credit Collection");
        collectionSectionLabel.setStyle("-fx-font-weight: bold;");
        TextField collectionInvoiceIdField = new TextField(); collectionInvoiceIdField.setPromptText("Invoice ID");
        TextField collectionAmountField = new TextField(); collectionAmountField.setPromptText("Amount Collected");
        Button recordCollectionButton = new Button("Record Collection");
        Label collectionStatusLabel = new Label();

        recordCollectionButton.setOnAction(e -> {
            try {
                String jsonBody = String.format("{\"amountCollected\":%s}", collectionAmountField.getText());
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/sales/invoice/" + collectionInvoiceIdField.getText() + "/collection"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    collectionStatusLabel.setStyle("-fx-text-fill: green;");
                    collectionStatusLabel.setText("Collection recorded.");
                } else {
                    collectionStatusLabel.setStyle("-fx-text-fill: red;");
                    collectionStatusLabel.setText("Failed: " + response.body());
                }
            } catch (Exception ex) {
                collectionStatusLabel.setStyle("-fx-text-fill: red;");
                collectionStatusLabel.setText("Error: " + ex.getMessage());
            }
        });

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.getChildren().addAll(
                backButton, titleLabel,
                customerSectionLabel, customerNameField, contactField, creditLimitField, addCustomerButton, customerStatusLabel,
                new Separator(),
                invoiceSectionLabel, invCustomerIdField, invOfficerIdField, paymentTypeBox, invProductIdField, invQtyField, invPriceField, createInvoiceButton, invoiceStatusLabel,
                new Separator(),
                collectionSectionLabel, collectionInvoiceIdField, collectionAmountField, recordCollectionButton, collectionStatusLabel
        );

        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);
        return new Scene(scrollPane, 480, 700);
    }

    public static void main(String[] args) {
        launch(args);
    }
}