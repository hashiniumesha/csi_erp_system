package com.csi.erpfrontend;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Application entry point. Screen-building logic lives in LoginView,
 * DashboardShell, and one class per module (QCView, InventoryView,
 * SalesView) — split out so more than one person can work on the frontend
 * at once without editing the same file.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Ceylon Sweets Island ERP - Login");
        primaryStage.setScene(LoginView.build(primaryStage));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
