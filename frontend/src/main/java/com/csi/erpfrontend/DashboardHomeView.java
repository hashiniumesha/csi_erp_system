package com.csi.erpfrontend;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
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

        javafx.scene.control.Button refreshButton = RefreshUtil.newButton();
        refreshButton.setOnAction(e -> AppNav.refreshCurrent());
        javafx.scene.layout.Region headerSpacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        javafx.scene.layout.HBox headerRow = new javafx.scene.layout.HBox(12, welcome, headerSpacer, refreshButton);
        headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        FlowPane statGrid = new FlowPane(16, 16);
        List<Node> charts = new ArrayList<>();
        boolean admin = Session.isAdmin();
        String role = Session.getRoleName();

        JSONObject summary = null;
        try {
            summary = ApiClient.getObject("/api/dashboard/summary");
            for (Stat stat : statsForRole(summary)) {
                statGrid.getChildren().add(buildStatCard(stat));
            }
        } catch (ApiClient.ApiException e) {
            Label error = new Label("Couldn't load dashboard summary: " + e.getMessage());
            error.getStyleClass().add("status-error");
            statGrid.getChildren().add(error);
        }

        // Charts below the stat cards — each pulls from a list endpoint
        // that already existed for its module, so no new backend work was
        // needed to add these. This is also where "Reporting & Analytics"
        // lives in this system: rather than a separate, largely-redundant
        // Reports screen, its "dashboard analytics" function (see the
        // client requirement document, section 3.6) is delivered directly
        // here, at the point every role already lands after logging in.
        if (admin || "QC Officer".equals(role)) {
            charts.add(buildGrnStatusChart());
        }
        if (admin || "Inventory Manager".equals(role)) {
            charts.add(buildStockLevelsChart());
            if (summary != null) {
                charts.add(buildInventoryCompositionChart(summary));
            }
        }
        if (admin || "Sales Officer".equals(role)) {
            charts.add(buildPaymentTypeChart());
        }

        // Same 2-column, fills-the-space grid as Inventory/Raw Materials,
        // except rows also grow vertically so the charts reach toward the
        // bottom of the screen instead of stopping at their own content
        // height with empty space left below.
        Node chartGrid = FormLayout.rowFillHeight(charts.toArray(new Node[0]));

        VBox layout = new VBox(20, headerRow, subtitle, statGrid, chartGrid);
        layout.setPadding(new Insets(28));
        VBox.setVgrow(chartGrid, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
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
            stats.add(new Stat("This Month's Sales", money(s.getDouble("salesTotalThisMonth")), "stat-card-cream"));
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

    private static VBox buildChartCard(String title, Node chart) {
        Label heading = new Label(title);
        heading.getStyleClass().add("section-title");
        VBox card = new VBox(10, heading, chart);
        card.getStyleClass().add("chart-card");
        // Width comes from the grid's percentage columns (FormLayout);
        // height grows to fill whatever the grid row gives this card,
        // rather than the chart stopping at a fixed pixel size.
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMaxHeight(Double.MAX_VALUE);
        chart.setStyle("-fx-max-width: infinity; -fx-max-height: infinity;");
        VBox.setVgrow(chart, Priority.ALWAYS);
        return card;
    }

    // QC: how the current batch of GRNs is split by decision status —
    // a quick read on whether raw material approvals are keeping up.
    private static VBox buildGrnStatusChart() {
        PieChart chart = new PieChart();
        chart.setLegendVisible(true);
        chart.setLabelsVisible(true);
        chart.setPrefHeight(360);
        try {
            JSONArray grns = ApiClient.getArray("/api/qc/grns");
            int pending = 0, approved = 0, rejected = 0;
            for (int i = 0; i < grns.length(); i++) {
                String status = grns.getJSONObject(i).optString("status", "");
                if ("Pending".equals(status)) pending++;
                else if ("Approved".equals(status)) approved++;
                else if ("Rejected".equals(status)) rejected++;
            }
            chart.getData().add(new PieChart.Data("Approved (" + approved + ")", approved));
            chart.getData().add(new PieChart.Data("Pending (" + pending + ")", pending));
            chart.getData().add(new PieChart.Data("Rejected (" + rejected + ")", rejected));
        } catch (ApiClient.ApiException e) {
            // Chart just renders empty; the stat cards above already
            // surfaced the "couldn't load" message once.
        }
        return buildChartCard("GRN Status", chart);
    }

    // Inventory: current stock against each material's own reorder level,
    // so a low-stock material is visually obvious next to a healthy one.
    private static VBox buildStockLevelsChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(true);
        chart.setAnimated(false);
        chart.setPrefHeight(360);
        XYChart.Series<String, Number> stockSeries = new XYChart.Series<>();
        stockSeries.setName("Current stock");
        XYChart.Series<String, Number> reorderSeries = new XYChart.Series<>();
        reorderSeries.setName("Reorder level");
        try {
            JSONArray materials = ApiClient.getArray("/api/raw-materials");
            int shown = 0;
            for (int i = 0; i < materials.length() && shown < 6; i++, shown++) {
                JSONObject m = materials.getJSONObject(i);
                String name = m.optString("name", "?");
                stockSeries.getData().add(new XYChart.Data<>(name, m.optDouble("currentStock", 0)));
                reorderSeries.getData().add(new XYChart.Data<>(name, m.optDouble("reorderLevel", 0)));
            }
            chart.getData().addAll(stockSeries, reorderSeries);
        } catch (ApiClient.ApiException e) {
            // Chart just renders empty.
        }
        return buildChartCard("Raw Material Stock Levels", chart);
    }

    // Inventory: how finished-product activity is distributed - available
    // stock, stock-in/out movement volume, and damage. "Deleted products"
    // was asked for too, but there is no delete-product feature anywhere
    // in this system, so that count doesn't actually exist - shown as real
    // recorded activity instead of inventing a number for it.
    private static VBox buildInventoryCompositionChart(JSONObject s) {
        PieChart chart = new PieChart();
        chart.setLegendVisible(true);
        chart.setLabelsVisible(true);
        chart.setPrefHeight(360);

        long available = s.optLong("totalFinishedProducts", 0);
        long stockIn = s.optLong("stockInMovementCount", 0);
        long stockOut = s.optLong("stockOutMovementCount", 0);
        long damaged = s.optLong("damagedProductRecordCount", 0);

        chart.getData().add(new PieChart.Data("Available Products (" + available + ")", available));
        chart.getData().add(new PieChart.Data("Stock-In Records (" + stockIn + ")", stockIn));
        chart.getData().add(new PieChart.Data("Stock-Out Records (" + stockOut + ")", stockOut));
        chart.getData().add(new PieChart.Data("Damaged Records (" + damaged + ")", damaged));

        return buildChartCard("Inventory Activity Composition", chart);
    }

    // Sales: how this period's invoices split between cash and credit.
    private static VBox buildPaymentTypeChart() {
        PieChart chart = new PieChart();
        chart.setLegendVisible(true);
        chart.setLabelsVisible(true);
        chart.setPrefHeight(360);
        try {
            JSONArray invoices = ApiClient.getArray("/api/sales/invoices");
            int cash = 0, credit = 0;
            for (int i = 0; i < invoices.length(); i++) {
                String type = invoices.getJSONObject(i).optString("paymentType", "");
                if ("Cash".equals(type)) cash++;
                else if ("Credit".equals(type)) credit++;
            }
            chart.getData().add(new PieChart.Data("Cash (" + cash + ")", cash));
            chart.getData().add(new PieChart.Data("Credit (" + credit + ")", credit));
        } catch (ApiClient.ApiException e) {
            // Chart just renders empty.
        }
        return buildChartCard("Invoices by Payment Type", chart);
    }
}