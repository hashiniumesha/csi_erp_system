package com.csi.erpfrontend;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.json.JSONObject;


public class AdminUsersView {

    private record Option(int id, String label) {
        @Override public String toString() { return label; }
    }

    public static Node build() {
        Label title = new Label("User Management");
        title.getStyleClass().add("page-title");

        VBox layout = new VBox(20, title, buildCreateUserCard(), buildUserListCard());
        layout.setPadding(new Insets(28));

        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("page-bg");
        return scrollPane;
    }

    private static VBox buildCreateUserCard() {
        Label sectionTitle = new Label("Create New User");
        sectionTitle.getStyleClass().add("section-title");

        TextField fullNameField = new TextField();
        fullNameField.setPromptText("e.g. Nimal Perera");
        TextField usernameField = new TextField();
        usernameField.setPromptText("e.g. nimal.perera");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Temporary password");

        ComboBox<Option> roleBox = new ComboBox<>();
        roleBox.setPromptText("Select role");
        roleBox.setMaxWidth(Double.MAX_VALUE);
        try {
            for (Object o : ApiClient.getArray("/api/roles")) {
                JSONObject r = (JSONObject) o;
                roleBox.getItems().add(new Option(r.getInt("roleId"), r.getString("roleName")));
            }
        } catch (ApiClient.ApiException e) {
            roleBox.setPromptText("Couldn't load roles");
        }

        Button createButton = new Button("Create User");
        createButton.getStyleClass().add("button-primary");
        Label statusLabel = newHiddenStatusLabel();

        createButton.setOnAction(e -> {
            if (fullNameField.getText().isBlank()) { showError(statusLabel, "Enter a full name."); return; }
            if (usernameField.getText().isBlank()) { showError(statusLabel, "Enter a username."); return; }
            if (passwordField.getText().isBlank() || passwordField.getText().length() < 6) {
                showError(statusLabel, "Enter a password of at least 6 characters."); return;
            }
            if (roleBox.getValue() == null) { showError(statusLabel, "Select a role."); return; }

            try {
                JSONObject body = new JSONObject();
                body.put("fullName", fullNameField.getText().trim());
                body.put("username", usernameField.getText().trim());
                body.put("password", passwordField.getText());
                body.put("roleId", roleBox.getValue().id());
                ApiClient.post("/api/users", body);
                showSuccess(statusLabel, "User created. They can now log in with that username and password.");
                fullNameField.clear(); usernameField.clear(); passwordField.clear(); roleBox.setValue(null);
            } catch (ApiClient.ApiException ex) {
                showError(statusLabel, ex.getMessage());
            }
        });

        VBox card = new VBox(10, sectionTitle,
                labeled("Full name", fullNameField), labeled("Username", usernameField),
                labeled("Temporary password", passwordField), labeled("Role", roleBox),
                createButton, statusLabel);
        card.getStyleClass().add("card");
        return card;
    }

    private static VBox buildUserListCard() {
        Label sectionTitle = new Label("Existing Users");
        sectionTitle.getStyleClass().add("section-title");

        VBox list = new VBox(8);
        try {
            for (Object o : ApiClient.getArray("/api/users")) {
                JSONObject u = (JSONObject) o;
                HBox row = new HBox(16);
                Label name = new Label(u.optString("fullName", "?"));
                name.setStyle("-fx-font-weight: bold; -fx-min-width: 160;");
                Label username = new Label("@" + u.optString("username", "?"));
                username.getStyleClass().add("muted-label");
                username.setStyle("-fx-min-width: 140;");
                Label role = new Label(u.optString("roleName", "?"));
                Label status = new Label(u.optString("status", "?"));
                status.getStyleClass().add("Active".equals(u.optString("status", "")) ? "status-success" : "status-error");
                row.getChildren().addAll(name, username, role, status);
                list.getChildren().add(row);
            }
        } catch (ApiClient.ApiException e) {
            Label error = new Label("Couldn't load users: " + e.getMessage());
            error.getStyleClass().add("status-error");
            list.getChildren().add(error);
        }

        VBox card = new VBox(10, sectionTitle, list);
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
}