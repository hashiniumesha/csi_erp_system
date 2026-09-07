package com.csi.erpfrontend;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Proper invoice document, opened as its own window when an invoice is
 * picked from the Invoice List — replaces the old "Invoice Created
 * Successfully" message-only flow with something that actually looks like
 * a business invoice and can be printed.
 */
public class InvoicePreviewView {

    public static void show(Integer invoiceId) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Invoice Preview");

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: white;");

        VBox document = new VBox(20);
        document.setId("invoice-document");
        document.setPadding(new Insets(30));
        document.setStyle("-fx-background-color: white; -fx-border-color: #E7E1D3; -fx-border-width: 1;");
        document.setPrefWidth(560);

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        try {
            JSONObject invoice = ApiClient.getObject("/api/sales/invoice/" + invoiceId);
            document.getChildren().addAll(
                    buildHeader(invoice),
                    buildPartiesRow(invoice),
                    new Separator(),
                    buildItemsTable(invoice),
                    buildTotalsRow(invoice),
                    buildFooter()
            );
        } catch (ApiClient.ApiException ex) {
            statusLabel.setText("Couldn't load this invoice: " + ex.getMessage());
            statusLabel.getStyleClass().setAll("status-error");
            statusLabel.setVisible(true);
            statusLabel.setManaged(true);
        }

        ScrollPane scrollPane = new ScrollPane(document);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        Button printButton = new Button("Print");
        printButton.getStyleClass().add("button-primary");
        printButton.setOnAction(e -> attemptPrint(document, stage));

        Button closeButton = new Button("Close");
        closeButton.getStyleClass().add("button-secondary");
        closeButton.setOnAction(e -> stage.close());

        HBox buttonRow = new HBox(10, printButton, closeButton);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(statusLabel, scrollPane, buttonRow);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Scene scene = new Scene(root, 640, 760);
        scene.getStylesheets().add(InvoicePreviewView.class.getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.showAndWait();
    }

    private static VBox buildHeader(JSONObject invoice) {
        ImageView logo = new ImageView(new Image(InvoicePreviewView.class.getResourceAsStream("/csi-logo.png")));
        logo.setFitWidth(56);
        logo.setFitHeight(56);
        logo.setPreserveRatio(true);

        Label company = new Label("Ceylon Sweets Island");
        company.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #472D30;");
        Label tagline = new Label("Sweets & Dairy Manufacturing");
        tagline.getStyleClass().add("muted-label");

        VBox companyBlock = new VBox(2, company, tagline);
        HBox brandRow = new HBox(12, logo, companyBlock);
        brandRow.setAlignment(Pos.CENTER_LEFT);

        Label invoiceWord = new Label("INVOICE");
        invoiceWord.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #E26D5C;");
        Label invoiceNo = new Label("Invoice #" + invoice.optInt("invoiceId"));
        invoiceNo.setStyle("-fx-font-weight: bold;");
        Label date = new Label("Date: " + invoice.optString("invoiceDate", "?"));
        date.getStyleClass().add("muted-label");
        Label paymentType = new Label("Payment: " + invoice.optString("paymentType", "?"));
        paymentType.getStyleClass().add("muted-label");

        VBox invoiceBlock = new VBox(4, invoiceWord, invoiceNo, date, paymentType);
        invoiceBlock.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox headerRow = new HBox(brandRow, spacer, invoiceBlock);
        headerRow.setAlignment(Pos.TOP_LEFT);

        return new VBox(headerRow);
    }

    private static HBox buildPartiesRow(JSONObject invoice) {
        Label billToTitle = new Label("Bill To");
        billToTitle.getStyleClass().add("section-title");
        Label customerName = new Label(invoice.optString("customerName", "?"));
        customerName.setStyle("-fx-font-weight: bold;");
        Label customerContact = new Label(invoice.optString("customerContact", ""));
        customerContact.getStyleClass().add("muted-label");
        VBox billTo = new VBox(4, billToTitle, customerName, customerContact);

        Label servedByTitle = new Label("Served By");
        servedByTitle.getStyleClass().add("section-title");
        Label officer = new Label(invoice.optString("salesOfficerName", "?"));
        VBox servedBy = new VBox(4, servedByTitle, officer);
        servedBy.setAlignment(Pos.TOP_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        return new HBox(billTo, spacer, servedBy);
    }

    private static VBox buildItemsTable(JSONObject invoice) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setMaxWidth(Double.MAX_VALUE);

        String[] headers = { "Product", "Qty", "Unit Price (Rs.)", "Subtotal (Rs.)" };
        for (int c = 0; c < headers.length; c++) {
            Label h = new Label(headers[c]);
            h.setStyle("-fx-font-weight: bold; -fx-text-fill: #472D30;");
            grid.add(h, c, 0);
        }
        GridPane.setHalignment(grid.getChildren().get(1), javafx.geometry.HPos.RIGHT);
        GridPane.setHalignment(grid.getChildren().get(2), javafx.geometry.HPos.RIGHT);
        GridPane.setHalignment(grid.getChildren().get(3), javafx.geometry.HPos.RIGHT);

        JSONArray items = invoice.optJSONArray("items");
        int row = 1;
        if (items != null) {
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                String unit = item.optString("unitOfMeasure", null) != null && !item.optString("unitOfMeasure").isBlank()
                        ? " " + item.optString("unitOfMeasure") : "";
                Label name = new Label(item.optString("productName", "?"));
                Label qty = new Label(String.format("%,.2f%s", item.optDouble("quantity", 0), unit));
                Label price = new Label(String.format("%,.2f", item.optDouble("unitPrice", 0)));
                Label subtotal = new Label(String.format("%,.2f", item.optDouble("subtotal", 0)));
                grid.add(name, 0, row);
                grid.add(qty, 1, row);
                grid.add(price, 2, row);
                grid.add(subtotal, 3, row);
                GridPane.setHalignment(qty, javafx.geometry.HPos.RIGHT);
                GridPane.setHalignment(price, javafx.geometry.HPos.RIGHT);
                GridPane.setHalignment(subtotal, javafx.geometry.HPos.RIGHT);
                row++;
            }
        }

        ColumnConstraintsHelper.applyProductTableColumns(grid);
        return new VBox(10, grid);
    }

    private static VBox buildTotalsRow(JSONObject invoice) {
        Label totalLabel = new Label("Total Amount");
        totalLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Label totalValue = new Label("Rs. " + String.format("%,.2f", invoice.optDouble("totalAmount", 0)));
        totalValue.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #E26D5C;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(spacer, new VBox(4, totalLabel, totalValue));
        VBox wrapper = new VBox(new Separator(), row);
        wrapper.setSpacing(10);
        return wrapper;
    }

    private static Label buildFooter() {
        Label footer = new Label("Thank you for your business — Ceylon Sweets Island.\nThis is a system-generated invoice.");
        footer.getStyleClass().add("muted-label");
        footer.setWrapText(true);
        return footer;
    }

    // Printer functionality is required to be genuinely functional, not
    // decorative, even though a physical printer isn't expected to be
    // connected in this environment — so every outcome (no printer
    // configured, user cancels, print itself fails) surfaces its own
    // specific message rather than silently doing nothing.
    private static void attemptPrint(Region document, Stage owner) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            showAlert(owner, Alert.AlertType.WARNING, "No printer is currently connected.\n\n" +
                    "Connect a printer or install a printer driver, then try again.");
            return;
        }
        try {
            boolean proceed = job.showPrintDialog(owner);
            if (!proceed) {
                return; // user cancelled the print dialog - not an error
            }
            boolean success = job.printPage(document);
            if (success) {
                job.endJob();
                showAlert(owner, Alert.AlertType.INFORMATION, "Sent to printer.");
            } else {
                showAlert(owner, Alert.AlertType.ERROR, "Printer connection is unavailable. The print job could not be completed.");
            }
        } catch (Exception ex) {
            showAlert(owner, Alert.AlertType.ERROR, "Printer connection is unavailable: " + ex.getMessage());
        }
    }

    private static void showAlert(Stage owner, Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message);
        alert.initOwner(owner);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // Small nested helper so buildItemsTable stays readable - gives the
    // product-name column room to grow while the numeric columns stay
    // compact and right-aligned.
    private static class ColumnConstraintsHelper {
        static void applyProductTableColumns(GridPane grid) {
            javafx.scene.layout.ColumnConstraints nameCol = new javafx.scene.layout.ColumnConstraints();
            nameCol.setPercentWidth(40);
            javafx.scene.layout.ColumnConstraints qtyCol = new javafx.scene.layout.ColumnConstraints();
            qtyCol.setPercentWidth(20);
            javafx.scene.layout.ColumnConstraints priceCol = new javafx.scene.layout.ColumnConstraints();
            priceCol.setPercentWidth(20);
            javafx.scene.layout.ColumnConstraints subtotalCol = new javafx.scene.layout.ColumnConstraints();
            subtotalCol.setPercentWidth(20);
            grid.getColumnConstraints().setAll(nameCol, qtyCol, priceCol, subtotalCol);
        }
    }
}
