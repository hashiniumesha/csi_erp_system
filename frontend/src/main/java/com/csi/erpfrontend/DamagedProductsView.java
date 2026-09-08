package com.csi.erpfrontend;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Read-only view of damage/wastage records, scoped to Admin + QC Officer
 * (see RoleAccessFilter's /api/damaged-products rule) - Inventory Manager
 * records damage via Inventory's "Record Damaged Product" card, but
 * reviewing the pattern of it is a QC/Admin concern in this system.
 */
public class DamagedProductsView {

    public static Node build() {
        HBox header = PageHeader.build("Damaged Products");

        VBox list = new VBox(10);
        try {
            JSONArray damaged = ApiClient.getArray("/api/damaged-products");
            if (damaged.isEmpty()) {
                Label empty = new Label("No damaged products recorded.");
                empty.getStyleClass().add("muted-label");
                list.getChildren().add(empty);
            }
            for (int i = 0; i < damaged.length(); i++) {
                list.getChildren().add(buildRow(damaged.getJSONObject(i)));
            }
        } catch (ApiClient.ApiException e) {
            Label error = new Label("Couldn't load damaged products: " + e.getMessage());
            error.getStyleClass().add("status-error");
            list.getChildren().add(error);
        }

        VBox listCard = new VBox(10, list);
        listCard.getStyleClass().add("card");

        VBox layout = new VBox(20, header, listCard);
        layout.setPadding(new Insets(28));

        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("page-bg");
        return scrollPane;
    }

    private static HBox buildRow(JSONObject d) {
        Label product = new Label(d.optString("productName", "?"));
        product.setStyle("-fx-font-weight: bold; -fx-min-width: 200;");
        Label quantity = new Label("Qty: " + d.optDouble("quantity", 0));
        quantity.setStyle("-fx-min-width: 80;");
        Label cause = new Label(d.optString("cause", "?"));
        cause.getStyleClass().add("muted-label");
        cause.setStyle("-fx-min-width: 180;");
        Label stage = new Label(d.optString("stage", "?"));
        stage.getStyleClass().add("muted-label");
        stage.setStyle("-fx-min-width: 120;");
        Label date = new Label(d.optString("damageDate", "?"));
        date.getStyleClass().add("muted-label");
        HBox row = new HBox(16, product, quantity, cause, stage, date);
        row.getStyleClass().add("data-row");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }
}
