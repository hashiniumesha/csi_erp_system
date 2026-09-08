package com.csi.erpfrontend;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.InputStream;

/**
 * Application entry point. Screen-building logic lives in LoginView,
 * DashboardShell, and one class per module (QCView, InventoryView,
 * SalesView) — split out so more than one person can work on the frontend
 * at once without editing the same file.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        loadFonts();
        primaryStage.getIcons().add(new Image(MainApp.class.getResourceAsStream("/csi-logo.png")));
        primaryStage.setTitle("Ceylon Sweets Island ERP - Login");
        primaryStage.setScene(LoginView.build(primaryStage));
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    // Montserrat isn't installed on a typical Windows machine, so it's
    // bundled as a font file and registered here at startup - style.css
    // then just references the family name "Montserrat" like any installed
    // font. Loaded once at whatever the JavaFX default size is; every
    // screen's own -fx-font-size in CSS still applies on top of this.
    private void loadFonts() {
        try (InputStream is = MainApp.class.getResourceAsStream("/fonts/Montserrat-Variable.ttf")) {
            if (is != null) {
                Font.loadFont(is, 13);
            }
        } catch (Exception ignored) {
            // Falls back to the CSS font-family stack's next entry
            // (Segoe UI) - never worth failing the app over.
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
