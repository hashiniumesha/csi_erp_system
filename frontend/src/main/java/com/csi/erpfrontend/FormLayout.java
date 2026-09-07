package com.csi.erpfrontend;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Two-column form layout shared by every Inventory and Raw Material card:
 * compact inputs on the left, a live-updating preview on the right instead
 * of one wide form filling the screen. Pulled out here since every card
 * that uses it follows the identical shape - build the inputs, build a
 * preview row per field, wrap with twoColumn().
 */
public class FormLayout {

    public static HBox twoColumn(VBox inputColumn, VBox previewCard) {
        inputColumn.setMinWidth(400);
        inputColumn.setMaxWidth(400);
        // Deliberately fixed, not grown to fill remaining space - a wide,
        // mostly-empty preview card read as unbalanced. Compact and to the
        // right of the (now wider) inputs instead.
        previewCard.setMinWidth(300);
        previewCard.setMaxWidth(320);
        HBox row = new HBox(20, inputColumn, previewCard);
        return row;
    }

    public static VBox previewCard(String title) {
        Label heading = new Label(title);
        heading.getStyleClass().add("section-title");
        VBox card = new VBox(10, heading);
        card.getStyleClass().addAll("card", "preview-card");
        return card;
    }

    // One "Label: value" row inside a preview card. Callers keep a
    // reference to the returned value Label and update its text from a
    // listener on the corresponding input - that's what makes it "live".
    public static HBox previewRow(String label, Label valueLabel) {
        Label labelNode = new Label(label + ":");
        labelNode.getStyleClass().add("field-label");
        labelNode.setMinWidth(120);
        valueLabel.getStyleClass().add("muted-label");
        valueLabel.setWrapText(true);
        valueLabel.setText("—");
        HBox row = new HBox(8, labelNode, valueLabel);
        HBox.setHgrow(valueLabel, Priority.ALWAYS);
        return row;
    }

    public static Label newPreviewValue() {
        Label label = new Label("—");
        return label;
    }
}
