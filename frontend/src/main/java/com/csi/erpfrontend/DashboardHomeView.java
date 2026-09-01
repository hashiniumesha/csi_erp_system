package com.csi.erpfrontend;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/*
  Dashboard landing page shown right after login, before a module is
  picked. Pulls one summary from GET /api/dashboard/summary and shows
 only the stat cards relevant to whoever is logged in.
 */
public class DashboardHomeView {

    private record Stat(String label, String value, String colorClass) {}

    public static Node build() {
        Label welcome = new Label("Welcome, " + (Session.getFullName() != null ? Session.getFullName() : "") + "!");
        welcome.getStyleClass().add("dashboard-welcome");

        Label subtitle = new Label(Session.getRoleName() + " dashboard — Ceylon Sweets Island ERP");
        subtitle.getStyleClass().add("dashboard-subtitle");

        FlowPane statGrid = new FlowPane(16, 16);

        try {
            JSONObject s = ApiClient.getObject("/api/dashboard/summary");
            for (Stat stat : statsForRole(s)) {
                statGrid.getChildren().add(buildStatCard(stat));
            }
        } catch (ApiClient.ApiException e) {
            Label error = new Label("Couldn't load dashboard summary: " + e.getMessage());
            error.getStyleClass().add("status-error");
            statGrid.getChildren().add(error);
        }

        VBox layout = new VBox(20, welcome, subtitle, statGrid);
        layout.setPadding(new Insets(28));

        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("page-bg");
        return scrollPane;
    }

    private static List<Stat> statsForRole(JSONObject s) {
        List<Stat> stats = new ArrayList<>();
        boolean admin = Session.isAdmin();
        String role = Session.getRoleName();

        if (admin || "QC Officer".equals(role)) {
            stats.add(new Stat("Pending GRNs", String.valueOf(s.getLong("pendingGrnCount")), "stat-card-terracotta"));
            stats.add(new Stat("Approved GRNs", String.valueOf(s.getLong("approvedGrnCount")), "stat-card-wine"));
        }
        if (admin || "Inventory Manager".equals(role)) {
            stats.add(new Stat("Finished Products", String.valueOf(s.getLong("totalFinishedProducts")), "stat-card-sage"));
            stats.add(new Stat("Low Stock Products", String.valueOf(s.getLong("lowStockFinishedProducts")), "stat-card-terracotta"));
            stats.add(new Stat("Raw Materials", String.valueOf(s.getLong("totalRawMaterials")), "stat-card-cream"));
            stats.add(new Stat("Low Stock Raw Materials", String.valueOf(s.getLong("lowStockRawMaterials")), "stat-card-terracotta"));
        }
        if (admin || "Sales Officer".equals(role)) {
            stats.add(new Stat("Customers", String.valueOf(s.getLong("totalCustomers")), "stat-card-sage"));
            stats.add(new Stat("Invoices Today", String.valueOf(s.getLong("invoicesToday")), "stat-card-cream"));
            stats.add(new Stat("Today's Sales", money(s.getDouble("salesTotalToday")), "stat-card-terracotta"));
            stats.add(new Stat("Outstanding Balance", money(s.getDouble("totalOutstandingBalance")), "stat-card-wine"));
        }
        if (admin) {
            stats.add(new Stat("Total Users", String.valueOf(s.getLong("totalUsers")), "stat-card-wine"));
        }
        return stats;
    }

    private static String money(double amount) {
        return "Rs. " + String.format("%,.2f", amount);
    }

    private static VBox buildStatCard(Stat stat) {
        Label value = new Label(stat.value());
        value.getStyleClass().add("stat-card-value");
        Label label = new Label(stat.label());
        label.getStyleClass().add("stat-card-label");

        VBox card = new VBox(6, value, label);
        card.getStyleClass().addAll("stat-card", stat.colorClass());
        return card;
    }
}