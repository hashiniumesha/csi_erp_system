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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Proper invoice document, opened as its own window when an invoice is
 * picked from the Invoice List — replaces the old "Invoice Created
 * Successfully" message-only flow with something that actually looks like
 * a business invoice, can be printed, and can be exported as a real PDF.
 *
 * Every row here is a plain HBox with fixed min-widths (the same pattern
 * used everywhere else in this app — SalesView's invoice list,
 * InventoryView's expiry rows) rather than a GridPane with percentage
 * columns. The GridPane version was intermittently leaving cells blank
 * (customer name, item rows) - this simpler layout doesn't have that
 * failure mode and is what's used everywhere else already.
 */
public class InvoicePreviewView {

    public static void show(Integer invoiceId) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Invoice Preview");

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("page-bg");

        VBox document = new VBox(18);
        document.getStyleClass().add("invoice-document");
        document.setPadding(new Insets(32));
        document.setPrefWidth(620);
        document.setMaxWidth(620);

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        Button printButton = new Button("Print");
        printButton.getStyleClass().add("button-primary");
        Button downloadButton = new Button("Download PDF");
        downloadButton.getStyleClass().add("button-secondary");
        Button closeButton = new Button("Close");
        closeButton.getStyleClass().add("button-secondary");
        closeButton.setOnAction(e -> stage.close());

        try {
            JSONObject invoice = ApiClient.getObject("/api/sales/invoice/" + invoiceId);
            document.getChildren().addAll(
                    buildHeader(invoice),
                    new Separator(),
                    buildPartiesRow(invoice),
                    new Separator(),
                    buildItemsTable(invoice),
                    buildTotalsRow(invoice),
                    buildFooter()
            );
            printButton.setOnAction(e -> attemptPrint(document, stage));
            downloadButton.setOnAction(e -> downloadPdf(invoiceId, stage, statusLabel));
        } catch (ApiClient.ApiException ex) {
            statusLabel.setText("Couldn't load this invoice: " + ex.getMessage());
            statusLabel.getStyleClass().setAll("status-error");
            statusLabel.setVisible(true);
            statusLabel.setManaged(true);
            printButton.setDisable(true);
            downloadButton.setDisable(true);
        }

        StackPaneWrap centered = new StackPaneWrap(document);

        ScrollPane scrollPane = new ScrollPane(centered.pane);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("invoice-scroll");

        HBox buttonRow = new HBox(10, printButton, downloadButton, closeButton);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(statusLabel, scrollPane, buttonRow);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Scene scene = new Scene(root, 720, 820);
        scene.getStylesheets().add(InvoicePreviewView.class.getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.showAndWait();
    }

    // Small wrapper so the invoice card sits centered in the scroll pane
    // instead of stretching edge to edge.
    private static class StackPaneWrap {
        final javafx.scene.layout.StackPane pane;
        StackPaneWrap(VBox document) {
            pane = new javafx.scene.layout.StackPane(document);
            pane.setPadding(new Insets(10));
        }
    }

    private static VBox buildHeader(JSONObject invoice) {
        ImageView logo = new ImageView(new Image(InvoicePreviewView.class.getResourceAsStream("/csi-logo.png")));
        logo.setFitWidth(56);
        logo.setFitHeight(56);
        logo.setPreserveRatio(true);

        Label company = new Label("Ceylon Sweets Island");
        company.getStyleClass().add("invoice-company-name");
        Label tagline = new Label("Sweets & Dairy Manufacturing");
        tagline.getStyleClass().add("muted-label");

        VBox companyBlock = new VBox(2, company, tagline);
        HBox brandRow = new HBox(12, logo, companyBlock);
        brandRow.setAlignment(Pos.CENTER_LEFT);

        Label invoiceWord = new Label("INVOICE");
        invoiceWord.getStyleClass().add("invoice-doc-title");
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

    private static VBox buildPartiesRow(JSONObject invoice) {
        Label billToTitle = new Label("Bill To");
        billToTitle.getStyleClass().add("section-title");
        Label customerName = new Label(invoice.optString("customerName", "—"));
        customerName.getStyleClass().add("invoice-party-name");
        Label customerContact = new Label(invoice.optString("customerContact", ""));
        customerContact.getStyleClass().add("muted-label");
        VBox billTo = new VBox(4, billToTitle, customerName, customerContact);

        Label servedByTitle = new Label("Served By");
        servedByTitle.getStyleClass().add("section-title");
        Label officer = new Label(invoice.optString("salesOfficerName", "—"));
        officer.getStyleClass().add("invoice-party-name");
        VBox servedBy = new VBox(4, servedByTitle, officer);

        HBox row = new HBox(40, billTo, servedBy);
        return new VBox(row);
    }

    private static VBox buildItemsTable(JSONObject invoice) {
        VBox table = new VBox(0);
        table.getStyleClass().add("invoice-items-table");

        HBox header = itemRow("Product", "Qty", "Unit Price (Rs.)", "Subtotal (Rs.)", true);
        table.getChildren().add(header);

        JSONArray items = invoice.optJSONArray("items");
        if (items == null || items.isEmpty()) {
            Label empty = new Label("No line items on this invoice.");
            empty.getStyleClass().add("muted-label");
            empty.setPadding(new Insets(10, 0, 10, 0));
            table.getChildren().add(empty);
        } else {
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                String unit = item.optString("unitOfMeasure", null) != null && !item.optString("unitOfMeasure").isBlank()
                        ? " " + item.optString("unitOfMeasure") : "";
                String productName = item.optString("productName", "?");
                String qty = String.format("%,.2f%s", item.optDouble("quantity", 0), unit);
                String price = String.format("%,.2f", item.optDouble("unitPrice", 0));
                String subtotal = String.format("%,.2f", item.optDouble("subtotal", 0));
                table.getChildren().add(itemRow(productName, qty, price, subtotal, false));
            }
        }

        return table;
    }

    private static HBox itemRow(String product, String qty, String price, String subtotal, boolean isHeader) {
        Label productLabel = new Label(product);
        productLabel.setMinWidth(230);
        productLabel.setMaxWidth(230);
        productLabel.setWrapText(true);
        Label qtyLabel = new Label(qty);
        qtyLabel.setMinWidth(100);
        qtyLabel.setAlignment(Pos.CENTER_RIGHT);
        Label priceLabel = new Label(price);
        priceLabel.setMinWidth(110);
        priceLabel.setAlignment(Pos.CENTER_RIGHT);
        Label subtotalLabel = new Label(subtotal);
        subtotalLabel.setMinWidth(110);
        subtotalLabel.setAlignment(Pos.CENTER_RIGHT);

        if (isHeader) {
            for (Label l : new Label[]{productLabel, qtyLabel, priceLabel, subtotalLabel}) {
                l.getStyleClass().add("invoice-table-header-cell");
            }
        } else {
            for (Label l : new Label[]{qtyLabel, priceLabel, subtotalLabel}) {
                l.getStyleClass().add("invoice-table-cell");
            }
            productLabel.getStyleClass().add("invoice-table-cell");
        }

        HBox row = new HBox(8, productLabel, qtyLabel, priceLabel, subtotalLabel);
        row.getStyleClass().add(isHeader ? "invoice-table-header-row" : "invoice-table-row");
        HBox.setHgrow(productLabel, Priority.ALWAYS);
        return row;
    }

    private static VBox buildTotalsRow(JSONObject invoice) {
        Label totalLabel = new Label("Total Amount");
        totalLabel.getStyleClass().add("invoice-total-label");
        Label totalValue = new Label("Rs. " + String.format("%,.2f", invoice.optDouble("totalAmount", 0)));
        totalValue.getStyleClass().add("invoice-total-value");

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

    private static void downloadPdf(Integer invoiceId, Stage stage, Label statusLabel) {
        try {
            byte[] pdfBytes = ApiClient.getBytes("/api/sales/invoice/" + invoiceId + "/pdf");

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Invoice");
            chooser.setInitialFileName("invoice-" + invoiceId + ".pdf");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
            File file = chooser.showSaveDialog(stage);

            if (file != null) {
                Files.write(file.toPath(), pdfBytes);
                showStatus(statusLabel, "Saved to " + file.getAbsolutePath(), true);
                try {
                    java.awt.Desktop.getDesktop().open(file);
                } catch (Exception openEx) {
                    // Saved fine either way - opening it automatically is a
                    // convenience, not something worth failing the action over.
                }
            }
        } catch (ApiClient.ApiException ex) {
            showStatus(statusLabel, ex.getMessage(), false);
        } catch (IOException ex) {
            showStatus(statusLabel, "Couldn't save the file: " + ex.getMessage(), false);
        }
    }

    private static void showStatus(Label label, String message, boolean success) {
        label.setText(message);
        label.getStyleClass().setAll(success ? "status-success" : "status-error");
        label.setVisible(true);
        label.setManaged(true);
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
                    "Connect a printer or install a printer driver, then try again. You can also use " +
                    "\"Download PDF\" and print that file from any PDF viewer.");
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
}
