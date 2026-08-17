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
                    String role = json.getJSONObject("role").getString("roleName");
                    statusLabel.setStyle("-fx-text-fill: green;");
                    statusLabel.setText("Login successful! Welcome " + fullName + " (" + role + ")");

                    primaryStage.setTitle("Ceylon Sweets Island ERP - QC Module");
                    primaryStage.setScene(buildQCScene());
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

    private Scene buildQCScene() {
        Label titleLabel = new Label("Production / QC Module");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

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
                titleLabel,
                grnSectionLabel, supplierIdField, rawMaterialIdField, quantityField, unitCostField, submitGrnButton, grnStatusLabel,
                new Separator(),
                decisionSectionLabel, grnIdField, qcOfficerIdField, parametersField, reasonField, approveButton, rejectButton, decisionStatusLabel
        );

        return new Scene(layout, 450, 600);
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

    public static void main(String[] args) {
        launch(args);
    }
}