package com.csi.erpfrontend;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Reports module (proposal section 5.7). Profit & Loss now covers Weekly,
 * Monthly, and Yearly periods on one report rather than three separate
 * report types - "generate the corresponding report" for whichever period
 * is selected. The sixth item from that section, "Graphical Dashboard", is
 * what the Dashboard screen already delivers, so it isn't repeated here.
 */
public class ReportsView {

    public static Node build() {
        HBox header = PageHeader.build("Reports");

        VBox layout = new VBox(20, header, buildProfitLossCard());
        layout.setPadding(new Insets(28));

        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("page-bg");
        return scrollPane;
    }

    private static VBox buildProfitLossCard() {
        Label sectionTitle = new Label("Profit & Loss Report");
        sectionTitle.getStyleClass().add("section-title");
        Label subtitle = new Label("Revenue, expenditure and net result for a week, month, or year — Admin access only");
        subtitle.getStyleClass().add("muted-label");

        ComboBox<String> periodTypeBox = new ComboBox<>();
        periodTypeBox.getItems().addAll("Weekly", "Monthly", "Yearly");
        periodTypeBox.setValue("Monthly");

        int currentYear = LocalDate.now().getYear();
        ComboBox<Integer> yearBox = new ComboBox<>();
        for (int y = currentYear - 2; y <= currentYear + 1; y++) yearBox.getItems().add(y);
        yearBox.setValue(currentYear);

        ComboBox<String> monthBox = new ComboBox<>();
        for (int m = 1; m <= 12; m++) {
            monthBox.getItems().add(java.time.Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        }
        monthBox.setValue(LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));

        DatePicker weekStartPicker = new DatePicker(LocalDate.now().with(DayOfWeek.MONDAY));

        VBox monthField = labeled("Month", monthBox);
        VBox yearField = labeled("Year", yearBox);
        VBox weekField = labeled("Week starting (Monday)", weekStartPicker);

        HBox pickerRow = new HBox(10, labeled("Period", periodTypeBox), yearField, monthField, weekField);

        Runnable updatePickerVisibility = () -> {
            String type = periodTypeBox.getValue();
            boolean isWeek = "Weekly".equals(type);
            boolean isMonth = "Monthly".equals(type);
            boolean isYear = "Yearly".equals(type);
            weekField.setVisible(isWeek); weekField.setManaged(isWeek);
            monthField.setVisible(isMonth); monthField.setManaged(isMonth);
            yearField.setVisible(isMonth || isYear); yearField.setManaged(isMonth || isYear);
        };
        periodTypeBox.valueProperty().addListener((obs, o, n) -> updatePickerVisibility.run());
        updatePickerVisibility.run();

        Button viewButton = new Button("View Report");
        viewButton.getStyleClass().add("button-primary");
        Button refreshButton = RefreshUtil.newButton();
        Button downloadButton = new Button("Download PDF");
        downloadButton.getStyleClass().add("button-secondary");

        Label statusLabel = newHiddenStatusLabel();

        VBox resultBox = new VBox(10);
        resultBox.setVisible(false);
        resultBox.setManaged(false);

        HBox buttonRow = new HBox(10, viewButton, refreshButton, downloadButton);

        VBox card = new VBox(10, sectionTitle, subtitle, pickerRow, buttonRow, statusLabel, resultBox);
        card.getStyleClass().add("card");

        Runnable loadReport = () -> {
            String query = buildQuery(periodTypeBox, yearBox, monthBox, weekStartPicker);
            try {
                JSONObject report = ApiClient.getObject("/api/reports/profit-loss" + query);
                resultBox.getChildren().setAll(buildReportSummary(report));
                resultBox.setVisible(true);
                resultBox.setManaged(true);
                statusLabel.setVisible(false);
                statusLabel.setManaged(false);
            } catch (ApiClient.ApiException ex) {
                showError(statusLabel, ex.getMessage());
                resultBox.setVisible(false);
                resultBox.setManaged(false);
            }
        };

        viewButton.setOnAction(e -> loadReport.run());
        refreshButton.setOnAction(e -> {
            loadReport.run();
            RefreshUtil.flash(refreshButton);
        });

        downloadButton.setOnAction(e -> {
            String query = buildQuery(periodTypeBox, yearBox, monthBox, weekStartPicker);
            try {
                byte[] pdfBytes = ApiClient.getBytes("/api/reports/profit-loss/pdf" + query);

                FileChooser chooser = new FileChooser();
                chooser.setTitle("Save Profit & Loss Report");
                chooser.setInitialFileName("profit-loss-" + periodTypeBox.getValue().toLowerCase(Locale.ENGLISH) + ".pdf");
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
                Stage stage = (Stage) downloadButton.getScene().getWindow();
                File file = chooser.showSaveDialog(stage);

                if (file != null) {
                    Files.write(file.toPath(), pdfBytes);
                    showSuccess(statusLabel, "Saved to " + file.getAbsolutePath());
                    try {
                        java.awt.Desktop.getDesktop().open(file);
                    } catch (Exception openEx) {
                        // Saved fine either way - opening it automatically is a
                        // convenience, not something worth failing the action over.
                    }
                }
            } catch (ApiClient.ApiException ex) {
                showError(statusLabel, ex.getMessage());
            } catch (IOException ex) {
                showError(statusLabel, "Couldn't save the file: " + ex.getMessage());
            }
        });

        return card;
    }

    private static String buildQuery(ComboBox<String> periodTypeBox, ComboBox<Integer> yearBox,
                                      ComboBox<String> monthBox, DatePicker weekStartPicker) {
        String type = periodTypeBox.getValue();
        if ("Weekly".equals(type)) {
            LocalDate weekStart = weekStartPicker.getValue() != null ? weekStartPicker.getValue() : LocalDate.now();
            return "?periodType=WEEK&weekStart=" + weekStart;
        }
        if ("Yearly".equals(type)) {
            return "?periodType=YEAR&year=" + yearBox.getValue();
        }
        int month = monthBox.getSelectionModel().getSelectedIndex() + 1;
        return "?periodType=MONTH&year=" + yearBox.getValue() + "&month=" + month;
    }

    private static VBox buildReportSummary(JSONObject report) {
        Label period = new Label(report.optString("periodLabel", "?"));
        period.getStyleClass().add("section-title");

        FlowPaneStats stats = new FlowPaneStats(
                report.optDouble("totalRevenue", 0),
                report.optDouble("totalExpenditure", 0),
                report.optDouble("netResult", 0)
        );

        VBox dailyList = new VBox(4);
        JSONArray breakdown = report.optJSONArray("breakdown");
        if (breakdown == null || breakdown.isEmpty()) {
            Label empty = new Label("No revenue or expenditure recorded in this period.");
            empty.getStyleClass().add("muted-label");
            dailyList.getChildren().add(empty);
        } else {
            for (int i = 0; i < breakdown.length(); i++) {
                JSONObject d = breakdown.getJSONObject(i);
                Label row = new Label(String.format("%-12s   Revenue: Rs. %,.2f   Expenditure: Rs. %,.2f   Net: Rs. %,.2f",
                        d.optString("label"), d.optDouble("revenue"), d.optDouble("expenditure"), d.optDouble("netResult")));
                row.getStyleClass().add("muted-label");
                dailyList.getChildren().add(row);
            }
        }

        return new VBox(10, period, stats.row, dailyList);
    }

    private static class FlowPaneStats {
        final HBox row;
        FlowPaneStats(double revenue, double expenditure, double net) {
            row = new HBox(16,
                    stat("Total Revenue", revenue, "stat-card-sage"),
                    stat("Total Expenditure", expenditure, "stat-card-terracotta"),
                    stat(net >= 0 ? "Net Profit" : "Net Loss", Math.abs(net), net >= 0 ? "stat-card-cream" : "stat-card-wine"));
        }

        private VBox stat(String label, double amount, String colorClass) {
            Label value = new Label("Rs. " + String.format("%,.2f", amount));
            value.getStyleClass().add("stat-card-value");
            Label labelNode = new Label(label);
            labelNode.getStyleClass().add("stat-card-label");
            VBox card = new VBox(6, value, labelNode);
            card.getStyleClass().addAll("stat-card", colorClass);
            return card;
        }
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
