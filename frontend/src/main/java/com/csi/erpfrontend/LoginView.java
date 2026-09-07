package com.csi.erpfrontend;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.json.JSONObject;

public class LoginView {

    public static Scene build(Stage stage) {
        ImageView logo = new ImageView(new Image(LoginView.class.getResourceAsStream("/csi-logo.png")));
        logo.setFitWidth(64);
        logo.setFitHeight(64);
        logo.setPreserveRatio(true);

        Label brand = new Label("Ceylon Sweets Island");
        brand.getStyleClass().add("page-title");

        Label subtitle = new Label("ERP System");
        subtitle.getStyleClass().add("muted-label");

        // Explicit widths here, rather than the shared .text-field default -
        // this login card is much narrower than the Inventory/Raw Material
        // forms that width was tuned for, and combined with the eye button
        // it doesn't fit otherwise.
        Label usernameLabel = new Label("Username");
        usernameLabel.getStyleClass().add("field-label");
        TextField usernameField = new TextField();
        usernameField.setPromptText("e.g. qcofficer");
        usernameField.setMaxWidth(300);

        Label passwordLabel = new Label("Password");
        passwordLabel.getStyleClass().add("field-label");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.setMaxWidth(Double.MAX_VALUE);

        // A PasswordField can't reveal its own text - overlaying a plain
        // TextField (bound to the same text) and toggling which one is
        // visible/managed is the standard JavaFX way to do a show/hide
        // password button.
        TextField passwordVisibleField = new TextField();
        passwordVisibleField.setPromptText("Enter your password");
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
        passwordVisibleField.setMaxWidth(Double.MAX_VALUE);
        passwordVisibleField.setVisible(false);
        passwordVisibleField.setManaged(false);

        StackPane passwordStack = new StackPane(passwordField, passwordVisibleField);
        passwordStack.setMaxWidth(240);
        HBox.setHgrow(passwordStack, Priority.ALWAYS);

        Button toggleVisibilityButton = new Button("👁");
        toggleVisibilityButton.getStyleClass().add("button-secondary");
        toggleVisibilityButton.setTooltip(new Tooltip("Show password"));
        toggleVisibilityButton.setOnAction(e -> {
            boolean nowVisible = !passwordVisibleField.isVisible();
            passwordVisibleField.setVisible(nowVisible);
            passwordVisibleField.setManaged(nowVisible);
            passwordField.setVisible(!nowVisible);
            passwordField.setManaged(!nowVisible);
            toggleVisibilityButton.getTooltip().setText(nowVisible ? "Hide password" : "Show password");
        });

        HBox passwordRow = new HBox(6, passwordStack, toggleVisibilityButton);

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
                logo, brand, subtitle,
                spacer(10),
                usernameLabel, usernameField,
                passwordLabel, passwordRow,
                loginButton,
                statusLabel
        );
        card.getStyleClass().add("card");
        card.setMaxWidth(380);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(28));

        StackPane root = new StackPane(card);
        root.getStyleClass().add("page-bg");
        root.setPadding(new Insets(40));

        Scene scene = new Scene(root, 520, 480);
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
