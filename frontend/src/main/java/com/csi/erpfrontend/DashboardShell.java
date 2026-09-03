package com.csi.erpfrontend;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Persistent sidebar + swappable content area, shared by every role.
 *
 * Which nav buttons appear is driven entirely by Session.getRoleName():
 * Admin sees every module, everyone else sees only the module their role
 * owns. This is the frontend half of role-based access control — the
 * backend's RBAC guard is what actually enforces it if a screen is reached
 * some other way.
 */
public class DashboardShell {

    public static Scene build(Stage stage) {
        StackPane content = new StackPane();
        content.getStyleClass().add("page-bg");

        Map<String, Supplier<Node>> modules = new LinkedHashMap<>();
        String role = Session.getRoleName();
        boolean admin = Session.isAdmin();

        modules.put("Dashboard", DashboardHomeView::build);

        if (admin) {
            modules.put("Users", AdminUsersView::build);
        }

        if (admin || "QC Officer".equals(role)) {
            modules.put("Production / QC", QCView::build);
        }
        if (admin || "Inventory Manager".equals(role)) {
            modules.put("Inventory", InventoryView::build);
        }
        if (admin || "Sales Officer".equals(role)) {
            modules.put("Sales & Billing", SalesView::build);
        }

        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(220);

        Label brand = new Label("Ceylon Sweets Island");
        brand.getStyleClass().add("sidebar-brand");
        brand.setWrapText(true);
        brand.setPadding(new Insets(0, 24, 16, 24));

        Label avatar = new Label(initials(Session.getFullName()));
        avatar.getStyleClass().add("sidebar-avatar");
        avatar.setMinSize(44, 44);
        avatar.setMaxSize(44, 44);
        avatar.setAlignment(Pos.CENTER);

        Label fullName = new Label(Session.getFullName() != null ? Session.getFullName() : "");
        fullName.getStyleClass().add("sidebar-name");
        fullName.setWrapText(true);

        Label roleLabel = new Label(role != null ? role : "");
        roleLabel.getStyleClass().add("sidebar-role");

        VBox identityText = new VBox(2, fullName, roleLabel);
        HBox identity = new HBox(10, avatar, identityText);
        identity.setAlignment(Pos.CENTER_LEFT);
        identity.setPadding(new Insets(0, 24, 24, 24));

        sidebar.getChildren().addAll(brand, identity);

        Button[] activeButton = new Button[1];
        for (Map.Entry<String, Supplier<Node>> entry : modules.entrySet()) {
            Button navButton = new Button(entry.getKey());
            navButton.getStyleClass().add("sidebar-nav-button");
            navButton.setOnAction(e -> {
                content.getChildren().setAll(entry.getValue().get());
                if (activeButton[0] != null) {
                    activeButton[0].getStyleClass().remove("sidebar-nav-button-active");
                }
                navButton.getStyleClass().add("sidebar-nav-button-active");
                activeButton[0] = navButton;
            });
            sidebar.getChildren().add(navButton);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        Button logoutButton = new Button("Log out");
        logoutButton.getStyleClass().add("sidebar-nav-button");
        logoutButton.setOnAction(e -> {
            Session.clear();
            stage.setTitle("Ceylon Sweets Island ERP - Login");
            stage.setScene(LoginView.build(stage));
        });
        sidebar.getChildren().add(logoutButton);

        // Land on the first module the role has access to.
        if (!modules.isEmpty()) {
            String firstLabel = modules.keySet().iterator().next();
            for (Node node : sidebar.getChildren()) {
                if (node instanceof Button button && button.getText().equals(firstLabel)) {
                    button.fire();
                    break;
                }
            }
        } else {
            content.getChildren().setAll(new Label("No modules are assigned to your role yet."));
        }

        BorderPane root = new BorderPane();
        root.setLeft(sidebar);
        root.setCenter(content);

        Scene scene = new Scene(root, 1000, 680);
        scene.getStylesheets().add(DashboardShell.class.getResource("/style.css").toExternalForm());
        return scene;
    }

    // "Priyantha Silva" -> "PS". Falls back to a single "?" if the name is
    // missing rather than showing an empty circle.
    private static String initials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length && result.length() < 2; i++) {
            if (!parts[i].isEmpty()) result.append(Character.toUpperCase(parts[i].charAt(0)));
        }
        return result.length() > 0 ? result.toString() : "?";
    }
}
