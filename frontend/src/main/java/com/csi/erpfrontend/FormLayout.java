package com.csi.erpfrontend;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;

/**
 * Shared layout helpers for Inventory/Raw Materials/Dashboard: cards that
 * grow to fill the available space (percentage-based columns) rather than
 * fixed pixel widths, so the page balances at any window size instead of
 * leaving a wide empty margin next to a fixed-size card - this is what a
 * fixed px width can't do once the app opens fullscreen.
 */
public class FormLayout {

    // Input column and preview column both grow to fill whatever width
    // the grid cell holding this form actually has (62/38 split), rather
    // than fixed pixel widths - this is what lets two of these sit side by
    // side (see gridOfTwo()) and still balance at any window size.
    public static GridPane twoColumn(VBox inputColumn, VBox previewCard) {
        inputColumn.setMinWidth(260);
        inputColumn.setMaxWidth(Double.MAX_VALUE);
        previewCard.setMinWidth(200);
        previewCard.setMaxWidth(Double.MAX_VALUE);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints inputCol = new ColumnConstraints();
        inputCol.setPercentWidth(62);
        inputCol.setHgrow(Priority.ALWAYS);
        ColumnConstraints previewCol = new ColumnConstraints();
        previewCol.setPercentWidth(38);
        previewCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(inputCol, previewCol);

        grid.add(inputColumn, 0, 0);
        grid.add(previewCard, 1, 0);
        return grid;
    }

    // Arranges whole cards (each already a complete unit - a twoColumn()
    // form, a list card, a chart card) in a 2-column grid that fills the
    // full page width instead of stacking every card in one long column
    // with empty space beside each one. A lone trailing card (odd count)
    // spans both columns rather than leaving half a row empty.
    public static GridPane gridOfTwo(Node... cards) {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints col = new ColumnConstraints();
        col.setPercentWidth(50);
        col.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col, new ColumnConstraints());
        grid.getColumnConstraints().get(1).setPercentWidth(50);
        grid.getColumnConstraints().get(1).setHgrow(Priority.ALWAYS);

        for (int i = 0; i < cards.length; i++) {
            GridPane.setHgrow(cards[i], Priority.ALWAYS);
            GridPane.setVgrow(cards[i], Priority.ALWAYS);
            boolean isLastAlone = (i == cards.length - 1) && (cards.length % 2 == 1);
            if (isLastAlone) {
                grid.add(cards[i], 0, i / 2, 2, 1);
            } else {
                grid.add(cards[i], i % 2, i / 2);
            }
        }
        return grid;
    }

    // Same idea as gridOfTwo but every card also grows to fill available
    // vertical space, for a row of cards (e.g. dashboard charts) that
    // should stretch to the bottom of the screen rather than sitting at
    // their natural height with empty space below.
    public static GridPane rowFillHeight(Node... cards) {
        GridPane grid = gridOfTwo(cards);
        RowConstraints row = new RowConstraints();
        row.setVgrow(Priority.ALWAYS);
        grid.getRowConstraints().add(row);
        return grid;
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
