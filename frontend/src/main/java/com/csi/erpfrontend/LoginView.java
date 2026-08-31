package com.csi.erpfrontend;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.json.JSONObject;

public class LoginView {

    public static Scene build(Stage stage) {
        Label brand = new Label("Ceylon Sweets Island");
        brand.getStyleClass().add("page-title");

        Label subtitle = new Label("ERP System");
        subtitle.getStyleClass().add("muted-label");

        Label usernameLabel = new Label("Username");
        usernameLabel.getStyleClass().add("field-label");
        TextField usernameField = new TextField();
        usernameField.setPromptText("e.g. qcofficer");

        Label passwordLabel = new Label("Password");
        passwordLabel.getStyleClass().add("field-label");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        Button loginButton = new Button("Log in");
        loginButton.getStyleClass().add("button-primary");
        loginButton.setMaxWidth(Double.MAX_VALUE);

        Runnable attemptLogin = () -> {
            String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
            String password = passwordField.getText() == null ? "" : passwordField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                showError(statusLabel, "Enter both a username and a password.");
                return;
            }

            try {
                JSONObject body = new JSONObject();
                body.put("username", username);
                body.put("password", password);

                JSONObject response = ApiClient.post("/api/auth/login", body);

                Session.set(
                        response.getInt("userId"),
                        response.getString("username"),
                        response.getString("fullName"),
                        response.optString("roleName", null)
                );

                stage.setTitle("Ceylon Sweets Island ERP - " + Session.getFullName());
                stage.setScene(DashboardShell.build(stage));
            } catch (ApiClient.ApiException e) {
                showError(statusLabel, e.getMessage());
            }
        };

        loginButton.setOnAction(e -> attemptLogin.run());
        passwordField.setOnAction(e -> attemptLogin.run());

        VBox card = new VBox(14,
                brand, subtitle,
                spacer(10),
                usernameLabel, usernameField,
                passwordLabel, passwordField,
                loginButton,
                statusLabel
        );
        card.getStyleClass().add("card");
        card.setMaxWidth(340);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(28));

        StackPane root = new StackPane(card);
        root.getStyleClass().add("page-bg");
        root.setPadding(new Insets(40));

        Scene scene = new Scene(root, 480, 460);
        scene.getStylesheets().add(LoginView.class.getResource("/style.css").toExternalForm());
        return scene;
    }

    private static void showError(Label statusLabel, String message) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().setAll("status-error");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private static VBox spacer(double height) {
        VBox box = new VBox();
        box.setMinHeight(height);
        return box;
    }
}
