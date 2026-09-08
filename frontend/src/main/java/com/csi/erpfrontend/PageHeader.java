package com.csi.erpfrontend;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Every module's page title, with a Refresh button on the same row -
 * reloads whichever module is currently on screen (see AppNav) with the
 * latest data from the database, and briefly confirms it visually.
 */
final class PageHeader {
    private PageHeader() {}

    static HBox build(String titleText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("page-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshButton = RefreshUtil.newButton();
        refreshButton.setOnAction(e -> AppNav.refreshCurrent());

        HBox row = new HBox(12, title, spacer, refreshButton);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
